package net.phoenix.raidcompletions;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RaidCompletions implements ClientModInitializer {

    public static String getStringFromURL(String url) {
        try {
            return new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(new ShowRaidCompletionsCommand().build());
        });
    }

    public class ShowRaidCompletionsCommand {
        private final static String TAG = Formatting.YELLOW + "[" + Formatting.GOLD + "ZAMN" + Formatting.YELLOW + "] ";

        public LiteralArgumentBuilder<FabricClientCommandSource> build() {
            LiteralArgumentBuilder<FabricClientCommandSource> edrCommand = LiteralArgumentBuilder.<FabricClientCommandSource>literal("edr")
                    .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("player", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                List<String> onlinePlayerNames = new ArrayList<>();
                                MinecraftServer server = context.getSource().getPlayer().getServer();
                                assert (server != null);
                                onlinePlayerNames.add(context.getSource().getPlayer().getEntityName());
                                return CommandSource.suggestMatching(onlinePlayerNames, builder);

                            })
                            .executes(context -> run(context.getSource(), StringArgumentType.getString(context, "player"))));
            return edrCommand;
        }

        public int run(FabricClientCommandSource source, String playerName) {
            final Thread myThread = new Thread(() -> {
                List<String> raidNames = Arrays.asList("The Nameless Anomaly", "The Canyon Colossus",
                        "Orphion's Nexus of Light", "Nest of the Grootslangs");
                List<String> raidLabels = Arrays.asList("TNA", "TCC", "NOL", "NOTG");
                List<Integer> finalRaidCompletions = new ArrayList<>();

                source.sendFeedback(Text.literal(TAG)
                        .append(Text.translatable("commands.raidcompletions.show_raid_completions.header")
                                .styled(style -> style.withColor(Formatting.GOLD))
                                .append(Text.literal(playerName)
                                        .styled(style -> style.withBold(true).withColor(Formatting.RED)))));

                String result = getStringFromURL("https://api.wynncraft.com/v2/player/" + playerName + "/stats");

                for (int i = 0; i < raidNames.size(); i++) {
                    int completion = 0;

                    String regex = raidNames.get(i) + "\",\"completed\":([0-9]*)";
                    Matcher matcher = Pattern.compile(regex).matcher(result);
                    while (matcher.find()) {
                        completion += Integer.parseInt(matcher.group(1));
                    }

                    finalRaidCompletions.add(completion);

                    source.sendFeedback(Text.literal(Formatting.GOLD + "- ")
                            .append(Text.translatable("commands.raidcompletions.show_raid_completions.entry",
                                    Text.literal(raidLabels.get(i)).styled(style -> style.withColor(Formatting.AQUA)),
                                    Text.literal(": ").styled(style -> style.withColor(Formatting.AQUA)),
                                    Text.literal(String.valueOf(finalRaidCompletions.get(i)))
                                            .styled(style -> style.withColor(Formatting.GOLD)),
                                    Text.translatable("commands.raidcompletions.show_raid_completions.times")
                                            .styled(style -> style.withColor(Formatting.YELLOW)))));
                }

                int sum = finalRaidCompletions.stream().mapToInt(Integer::intValue).sum();
                source.sendFeedback(Text.literal(Formatting.GOLD + "- ")
                        .append(Text.translatable("commands.raidcompletions.show_raid_completions.total")
                                .styled(style -> style.withColor(Formatting.AQUA))
                                .append(Formatting.AQUA + ": ")
                                .append(Text.literal(Formatting.GOLD + String.valueOf(sum))
                                        .styled(style -> style.withColor(Formatting.GOLD))
                                )));
            });
            myThread.start();

            return 1;
        }
    }

}
