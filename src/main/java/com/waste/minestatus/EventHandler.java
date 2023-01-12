package com.waste.minestatus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;


public class EventHandler {
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event){
        System.out.println("bink");
        MineStatus.socket.sendText("[" + event.getUsername() + "] " + event.getMessage().getString(), true);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event){
        if(event.getEntity() instanceof ServerPlayer){
            MineStatus.socket.sendText(event.getEntity().getName().getString() + " joined the server", true);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event){
        if(event.getEntity() instanceof ServerPlayer){
            MineStatus.socket.sendText(event.getEntity().getName().getString() + " left the server", true);
        }
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        System.out.println("Config loaded");
        System.out.println("\n\n" + event.getConfig().getConfigData().get("websocket-hostname").equals("") + "\n\n");
    }
}
