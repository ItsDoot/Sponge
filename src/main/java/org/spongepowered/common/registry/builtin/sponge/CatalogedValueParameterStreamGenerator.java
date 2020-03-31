package org.spongepowered.common.registry.builtin.sponge;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import org.spongepowered.api.command.parameter.managed.standard.CatalogedValueParameter;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.dimension.Dimension;
import org.spongepowered.common.command.brigadier.argument.parser.StandardArgumentParser;
import org.spongepowered.common.command.parameter.managed.standard.BigDecimalValueParameter;
import org.spongepowered.common.command.parameter.managed.standard.BigIntegerValueParameter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CatalogedValueParameterStreamGenerator {

    private CatalogedValueParameterStreamGenerator() {
    }

    /*
      ArgumentTypes.register("brigadier:bool", BoolArgumentType.class, new ArgumentSerializer<>(BoolArgumentType::bool));
      ArgumentTypes.register("brigadier:float", FloatArgumentType.class, new FloatArgumentSerializer());
      ArgumentTypes.register("brigadier:double", DoubleArgumentType.class, new DoubleArgumentSerializer());
      ArgumentTypes.register("brigadier:integer", IntegerArgumentType.class, new IntArgumentSerializer());
      ArgumentTypes.register("brigadier:long", LongArgumentType.class, new LongArgumentSerializer());
      ArgumentTypes.register("brigadier:string", StringArgumentType.class, new StringArgumentSerializer());
      register("entity", EntityArgument.class, new EntityArgument.Serializer());
      register("game_profile", GameProfileArgument.class, new ArgumentSerializer<>(GameProfileArgument::gameProfile));
      register("block_pos", BlockPosArgument.class, new ArgumentSerializer<>(BlockPosArgument::blockPos));
      register("column_pos", ColumnPosArgument.class, new ArgumentSerializer<>(ColumnPosArgument::columnPos));
      register("vec3", Vec3Argument.class, new ArgumentSerializer<>(Vec3Argument::vec3));
      register("vec2", Vec2Argument.class, new ArgumentSerializer<>(Vec2Argument::vec2));
      register("block_state", BlockStateArgument.class, new ArgumentSerializer<>(BlockStateArgument::blockState));
      register("block_predicate", BlockPredicateArgument.class, new ArgumentSerializer<>(BlockPredicateArgument::blockPredicate));
      register("item_stack", ItemArgument.class, new ArgumentSerializer<>(ItemArgument::item));
      register("item_predicate", ItemPredicateArgument.class, new ArgumentSerializer<>(ItemPredicateArgument::itemPredicate));
      register("color", ColorArgument.class, new ArgumentSerializer<>(ColorArgument::color));
      register("component", ComponentArgument.class, new ArgumentSerializer<>(ComponentArgument::component));
      register("message", MessageArgument.class, new ArgumentSerializer<>(MessageArgument::message));
      register("nbt_compound_tag", NBTCompoundTagArgument.class, new ArgumentSerializer<>(NBTCompoundTagArgument::nbt));
      register("nbt_tag", NBTTagArgument.class, new ArgumentSerializer<>(NBTTagArgument::func_218085_a));
      register("nbt_path", NBTPathArgument.class, new ArgumentSerializer<>(NBTPathArgument::nbtPath));
      register("objective", ObjectiveArgument.class, new ArgumentSerializer<>(ObjectiveArgument::objective));
      register("objective_criteria", ObjectiveCriteriaArgument.class, new ArgumentSerializer<>(ObjectiveCriteriaArgument::objectiveCriteria));
      register("operation", OperationArgument.class, new ArgumentSerializer<>(OperationArgument::operation));
      register("particle", ParticleArgument.class, new ArgumentSerializer<>(ParticleArgument::particle));
      register("rotation", RotationArgument.class, new ArgumentSerializer<>(RotationArgument::rotation));
      register("scoreboard_slot", ScoreboardSlotArgument.class, new ArgumentSerializer<>(ScoreboardSlotArgument::scoreboardSlot));
      register("score_holder", ScoreHolderArgument.class, new ScoreHolderArgument.Serializer());
      register("swizzle", SwizzleArgument.class, new ArgumentSerializer<>(SwizzleArgument::swizzle));
      register("team", TeamArgument.class, new ArgumentSerializer<>(TeamArgument::team));
      register("item_slot", SlotArgument.class, new ArgumentSerializer<>(SlotArgument::slot));
      register("resource_location", ResourceLocationArgument.class, new ArgumentSerializer<>(ResourceLocationArgument::resourceLocation));
      register("mob_effect", PotionArgument.class, new ArgumentSerializer<>(PotionArgument::mobEffect));
      register("function", FunctionArgument.class, new ArgumentSerializer<>(FunctionArgument::func_200021_a));
      register("entity_anchor", EntityAnchorArgument.class, new ArgumentSerializer<>(EntityAnchorArgument::entityAnchor));
      register("int_range", IRangeArgument.IntRange.class, new IRangeArgument.IntRange.Serializer());
      register("float_range", IRangeArgument.FloatRange.class, new IRangeArgument.FloatRange.Serializer());
      register("item_enchantment", EnchantmentArgument.class, new ArgumentSerializer<>(EnchantmentArgument::enchantment));
      register("entity_summon", EntitySummonArgument.class, new ArgumentSerializer<>(EntitySummonArgument::entitySummon));
      register("dimension", DimensionArgument.class, new ArgumentSerializer<>(DimensionArgument::getDimension));
      register("time", TimeArgument.class, new ArgumentSerializer<>(TimeArgument::func_218091_a)); // duration in ticks
     */

    public static Stream<CatalogedValueParameter<?>> stream() {
        return Stream.of(
                new BigDecimalValueParameter(),
                new BigIntegerValueParameter(),
                StandardArgumentParser.createIdentity("boolean", BoolArgumentType.bool()),
                // SpongeArgumentTypeAdapter.createIdentity("color", ColorArgument.color()) // We don't want this.
                // Color
                // DataContainer
                // DateTime
                StandardArgumentParser.createCast("dimension", DimensionArgument.getDimension(), Dimension.class),
                StandardArgumentParser.createIdentity("double", DoubleArgumentType.doubleArg()),
                // Duration
                // This is for a single entity. We'll have a separate one for multiple.
                StandardArgumentParser.createConverter("entity", EntityArgument.entity(),
                        (cause, selector) -> (Entity) selector.selectOne(cause.getSource())),

                StandardArgumentParser.createConverter("many_entities", EntityArgument.entities(),
                        (cause, selector) -> selector.select(cause.getSource()).stream().map(x -> (Entity) x).collect(Collectors.toList())),
                StandardArgumentParser.createConverter("many_player", EntityArgument.players(),
                        (cause, selector) -> (Player) selector.selectPlayers(cause.getSource())),

                StandardArgumentParser.createConverter("player", EntityArgument.player(),
                        (cause, selector) -> (Player) selector.selectOnePlayer(cause.getSource()))
        );
    }

}
