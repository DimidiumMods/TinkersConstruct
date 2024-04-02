package slimeknights.tconstruct.library.modifiers.modules;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.json.predicate.tool.ToolContextPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import java.util.function.Function;

/**
 * Represents conditions for a modifier module, since this is reused across several modules
 */
public record ModifierModuleCondition(IJsonPredicate<IToolContext> tool, IntRange modifierLevel) {
  /** Instance matching any tool context predicate and any modifier level */
  public static final ModifierModuleCondition ANY = new ModifierModuleCondition(ToolContextPredicate.ANY, ModifierEntry.VALID_LEVEL);
  /** Loadable for modifier module conditions, typically used with {@link RecordLoadable#directField(Function)} */
  public static final RecordLoadable<ModifierModuleCondition> LOADABLE = RecordLoadable.create(
    ToolContextPredicate.LOADER.defaultField("tool", ModifierModuleCondition::tool),
    ModifierEntry.VALID_LEVEL.defaultField("modifier_level", ModifierModuleCondition::modifierLevel),
    ModifierModuleCondition::new);
  /** Generic field instance used for most modules with conditions */
  public static final LoadableField<ModifierModuleCondition,ConditionalModifierModule> FIELD = LOADABLE.directField(ConditionalModifierModule::condition);

  /** Swaps the tool condition for the passed condition */
  public ModifierModuleCondition with(IJsonPredicate<IToolContext> tool) {
    return new ModifierModuleCondition(tool, this.modifierLevel);
  }

  /** Swaps the modifier level condition for the passed condition */
  public ModifierModuleCondition with(IntRange modifierLevel) {
    return new ModifierModuleCondition(this.tool, modifierLevel);
  }

  /** Validates that the tool and modifier pass the conditions */
  public boolean matches(IToolContext tool, ModifierEntry modifier) {
    return this.modifierLevel.test(modifier.getLevel()) && this.tool.matches(tool);
  }


  /* JSON */

  /** Serializes these conditions into the given parent object */
  public void serializeInto(JsonObject parent) {
    if (this.tool != ToolContextPredicate.ANY) {
      parent.add("tool", ToolContextPredicate.LOADER.serialize(this.tool));
    }
    ModifierEntry.VALID_LEVEL.serializeInto(parent, "modifier_level", modifierLevel);
  }

  /** Deserializes these objects from the given parent object */
  public static ModifierModuleCondition deserializeFrom(JsonObject parent) {
    IJsonPredicate<IToolContext> tool = ToolContextPredicate.LOADER.getOrDefault(parent, "tool");
    IntRange modifierLevel = ModifierEntry.VALID_LEVEL.getOrDefault(parent, "modifier_level");
    return new ModifierModuleCondition(tool, modifierLevel);
  }


  /* Network */

  /** Writes this object to the network */
  public void toNetwork(FriendlyByteBuf buffer) {
    ToolContextPredicate.LOADER.encode(buffer, this.tool);
    this.modifierLevel.toNetwork(buffer);
  }

  /** Reads this object from the network */
  public static ModifierModuleCondition fromNetwork(FriendlyByteBuf buffer) {
    IJsonPredicate<IToolContext> tool = ToolContextPredicate.LOADER.decode(buffer);
    IntRange modifierLevel = IntRange.fromNetwork(buffer);
    return new ModifierModuleCondition(tool, modifierLevel);
  }


  /** Simple interface to allow a module to use  */
  public interface ConditionalModifierModule {
    ModifierModuleCondition condition();
  }

  /* Builder */

  /** Generic builder that supports setting conditions */
  public static abstract class Builder<T extends Builder<T>> {
    /** Condition to use in the final build method */
    protected ModifierModuleCondition condition = ModifierModuleCondition.ANY;

    /** Gets this casted to the generic type */
    @SuppressWarnings("unchecked")
    private T setCondition(ModifierModuleCondition condition) {
      this.condition = condition;
      return (T) this;
    }


    /* Tools */

    /** Sets the tool condition for this module */
    public T tool(IJsonPredicate<IToolContext> tool) {
      return setCondition(this.condition.with(tool));
    }

    /** Sets the tool condition for this module */
    public T toolItem(IJsonPredicate<Item> tool) {
      return tool(ToolContextPredicate.fallback(tool));
    }


    /* Level range */

    /** Sets the level range for this builder */
    private T setLevels(IntRange range) {
      return setCondition(this.condition.with(range));
    }

    /** Sets the modifier level range for this module */
    public T levelRange(int min, int max) {
      return setLevels(ModifierEntry.VALID_LEVEL.range(min, max));
    }

    /** Sets the modifier level range for this module */
    public T minLevel(int min) {
      return setLevels(ModifierEntry.VALID_LEVEL.min(min));
    }

    /** Sets the modifier level range for this module */
    public T maxLevel(int max) {
      return setLevels(ModifierEntry.VALID_LEVEL.max(max));
    }

    /** Sets the modifier level range for this module */
    public T exactLevel(int value) {
      return setLevels(ModifierEntry.VALID_LEVEL.exactly(value));
    }
  }
}
