package eu.pb4.placeholderstest.mixin;

import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.impl.predicates.compat.PermissionPredicate;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Shadow
    @Final
    private MinecraftServer server;
    @Unique
    private ServerPlayerEntity temporaryPlayer = null;

    @Inject(method = "onPlayerConnect", at = @At(value = "HEAD"))
    private void predicate_api_storePlayer(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        this.temporaryPlayer = player;
    }

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void predicate_api_removeStoredPlayer(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        this.temporaryPlayer = null;
    }

    @ModifyArg(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    private Text predicate_api_check_permission(Text text) {
        boolean hasPermission = new PermissionPredicate("group.test", -1).test(PredicateContext.of(temporaryPlayer)).success();

        return Text.literal(hasPermission ? text.getString() : "You do not have permission to join this server!");
    }

}
