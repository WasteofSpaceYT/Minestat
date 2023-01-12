package com.waste.minestatus;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingPlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MineStatus.MODID)
public class MineStatus {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "minestatus";
    public static final String hostName = "localhost";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    public static ForgeConfigSpec.ConfigValue<String> token;

    private static MinecraftServer server;
    public static WebSocket socket;
    public static void ConfigInit(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        ForgeConfigSpec.ConfigValue<String> hostname = builder.comment("The hostname of the Discord bot's websocket server")
                .define("websocket-hostname", "");
        ForgeConfigSpec.ConfigValue<String> socketPort = builder.comment("The port for the Dicord bot's websocket server")
                .define("websocket-port", "");
    }

    public static final ForgeConfigSpec CONFIG_SPEC;
    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ConfigInit(builder);
        CONFIG_SPEC = builder.build();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIG_SPEC);
    }

    public MineStatus() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        //modEventBus.addListener(chatHandler::onServerChat);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        server = event.getServer();
        if(true){

        try {
            socket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://" + hostName + ":6969"), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    return null;
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(9426), 0);
            server.createContext("/", new ChatHandler());
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }

    class ChatHandler implements HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange t) throws IOException {
            String response = "";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            InputStream is = t.getRequestBody();
            try {
                int in = -1;
                String s = "";
                while ((in = is.read()) != -1) {
                    s += (char) in;
                }
                if(s.equals("playercount")){
                    String finalS = s;
                    response = String.valueOf(server.getPlayerCount());
                    os.write(response.getBytes());
                    os.close();
                }
                if(s.equals("playerlist")){
                    String finalS = s;
                    response = String.valueOf(Arrays.stream(server.getPlayerList().getPlayerNamesArray()).toList().toString());
                    os.write(response.getBytes());
                    os.close();
                }
                if(s.equals("wsconnect")){
                    String finalS = s;
                    socket = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://" + hostName + ":6969"), new WebSocket.Listener() {
                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            return null;
                        }
                    }).get();
                    response = "Connected to websocket";
                    os.write(response.getBytes());
                    os.close();
                }
                if(s.startsWith("message")){
                    String finalS = s.substring(8);
                    List<ServerPlayer> players = server.getPlayerList().getPlayers();
                    for(ServerPlayer player : players){
                        player.sendSystemMessage(Component.nullToEmpty(finalS));
                    }
                    os.close();
                }
                if(s.startsWith("execute")){
                    String finalS = s.substring(8);
                    server.getCommands().getDispatcher().execute(finalS, server.createCommandSourceStack());
                    os.close();
                }
            } catch (IOException exp) {
                exp.printStackTrace();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
