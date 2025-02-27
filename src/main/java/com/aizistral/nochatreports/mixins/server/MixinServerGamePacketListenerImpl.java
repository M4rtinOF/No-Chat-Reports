package com.aizistral.nochatreports.mixins.server;

import com.aizistral.nochatreports.core.NoReportsConfig;

import net.minecraft.core.Registry;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.ChatTypeDecoration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerChatHeaderPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl implements ServerPlayerConnection {
	@Shadow
	public ServerPlayer player;

	/**
	 * @reason Convert player message to system message if mod is configured respectively.
	 * This allows to circumvent signature check on client, as it only checks player messages.
	 * @author JFronny (original implementation)
	 * @author Aizistral
	 */

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void onSend(Packet<?> packet, CallbackInfo info) {
		if (NoReportsConfig.convertToGameMessage()) {
			if (packet instanceof ClientboundPlayerChatHeaderPacket) {
				info.cancel();
			} else if (packet instanceof ClientboundPlayerChatPacket chat) {
				packet = new ClientboundSystemChatPacket(chat.chatType().resolve(this.player.level.registryAccess())
						.get().decorate(chat.message().serverContent()), false);


				info.cancel();
				this.send(packet);
			}
		}
	}

	/**
	 * @reason Ensure conversion works regardless of which send method is used.
	 * @author Aizistral
	 */

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
			at = @At("HEAD"), cancellable = true)
	private void onSend(Packet<?> packet, @Nullable PacketSendListener packetSendListener, CallbackInfo info) {
		if (NoReportsConfig.convertToGameMessage()) {
			if (packet instanceof ClientboundPlayerChatHeaderPacket) {
				info.cancel();
			} else if (packet instanceof ClientboundPlayerChatPacket chat && packetSendListener != null) {
				info.cancel();
				((ServerGamePacketListenerImpl) (Object) this).send(chat);
			}
		}
	}

}
