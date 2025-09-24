package eu.pb4.predicate.impl.predicates.compat;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.predicate.api.AbstractPredicate;
import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.api.PredicateResult;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.PermissionLevelSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public final class PermissionPredicate extends AbstractPredicate {
    public static final Identifier ID = Identifier.of("permission");
    public static final MapCodec<PermissionPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("permission").forGetter(PermissionPredicate::permission),
            Codec.INT.optionalFieldOf("operator", -1).forGetter(PermissionPredicate::operator)
    ).apply(instance, PermissionPredicate::new));

    private static final Field luckPermsUserField;

    static {
        try {
            luckPermsUserField = ServerPlayerEntity.class.getDeclaredField("luckperms$user");
            luckPermsUserField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private final String permission;
    private final int operator;

    public PermissionPredicate(String permission, int operator) {
        super(ID, CODEC);
        this.permission = permission;
        this.operator = operator;
    }

    public String permission() {
        return this.permission;
    }

    private int operator() {
        return this.operator;
    }

    @Override
    public PredicateResult<?> test(PredicateContext context) {
        final ServerCommandSource source = context.source();
        if (context.hasGameProfile()) {
            final GameProfile profile = context.gameProfile();
            assert profile != null;

            if (operator == -1) {
                CompletableFuture<Boolean> hasPermissionFuture = Permissions.check(profile.getId(), this.permission);
                return PredicateResult.ofBoolean(hasPermissionFuture.join());
            }


            CompletableFuture<Boolean> hasPermission = Permissions.getPermissionValue(profile.getId(), this.permission)
                    .thenApply(state -> state.orElseGet(() ->
                            source instanceof PermissionLevelSource permissionLevelSource ?
                                    permissionLevelSource.hasPermissionLevel(operator)
                                    : operator == 0));

            return PredicateResult.ofBoolean(hasPermission.join());
        }

        return PredicateResult.ofBoolean(operator == -1 ? Permissions.check(source, this.permission) : Permissions.check(source, this.permission, this.operator));
    }
}
