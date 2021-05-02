package io.github.codeutilities.audio;

import io.github.codeutilities.CodeUtilities;
import io.github.codeutilities.config.CodeUtilsConfig;
import io.github.codeutilities.util.ILoader;
import io.github.codeutilities.util.render.ToasterUtil;
import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundCategory;
import org.json.JSONObject;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioHandler implements ILoader {
    String currentPlotId = "";
    HashMap<String, HashSet<MediaPlayer>> tracks = new HashMap<>();
    private static AudioHandler instance;
    public AudioHandler() {
        instance = this;
    }
    public static AudioHandler getInstance() {
        return instance;
    }
    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
    @Override
    public void load() {
        if(!CodeUtilsConfig.getBool("audio")) { return; }
        try {
            com.sun.javafx.application.PlatformImpl.startup(() -> {
                URI uri = URI.create(CodeUtilsConfig.getStr("audioUrl"));
                String username = MinecraftClient.getInstance().getSession().getUsername();
                IO.Options options = IO.Options.builder()
                        .setQuery("username="+username+"&source="+("fabric-"+CodeUtilities.MOD_ID+"-"+CodeUtilities.MOD_VERSION))
                        .build();
                Socket socket = IO.socket(uri, options);
                socket.on("message", args -> {
                    JSONObject obj = new JSONObject(args[0].toString());
                    String action = (String) obj.get("action");
                    if (action.equals("play")) {
                        try {
                            String plotId = (String) obj.get("plot");
                            String track = (String) obj.get("track");
                            String source = (String) obj.get("source");
                            String title = (String) obj.get("title");
                            boolean loop = (boolean) obj.get("loop");
                            if (!currentPlotId.equals(plotId)) {
                                // enable to show when plot takes control of session
                                if(CodeUtilsConfig.getBool("audioAlerts")) {
                                    ToasterUtil.sendToaster("Now Playing", "Plot " + plotId, SystemToast.Type.NARRATOR_TOGGLE);
                                }
                                currentPlotId = plotId;
                            }
                            if(CodeUtilities.BETA) {
                                ToasterUtil.sendToaster("Debug Playback - "+plotId,title,SystemToast.Type.NARRATOR_TOGGLE);
                            }
                            Media mediaSource = new Media(source);
                            MediaPlayer player = new MediaPlayer(mediaSource);
                            if(loop) {
                                player.setOnEndOfMedia(new Runnable() {
                                    @Override
                                    public void run() {
                                        player.seek(Duration.ZERO);
                                        player.play();
                                    }
                                });
                            }
                            player.play();
                            HashSet<MediaPlayer> clips = tracks.get(track);
                            if (clips == null) {
                                clips = new HashSet<>();
                            }
                            clips.add(player);
                            tracks.put(track, clips);
                        } catch (
                                Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (action.equals("stop")) {
                        String plotId = (String) obj.get("plot");
                        String track = (String) obj.get("track");
                        if (!currentPlotId.equals(plotId)) {
                            ToasterUtil.sendToaster("Now Playing", "Plot " + plotId, SystemToast.Type.NARRATOR_TOGGLE);
                            currentPlotId = plotId;
                        }
                        if (track.equals("all")) {
                            for (Map.Entry<String, HashSet<MediaPlayer>> trackList : tracks.entrySet()) {
                                for (MediaPlayer audio : trackList.getValue()) {
                                    audio.stop();
                                }
                            }
                        }
                        HashSet<MediaPlayer> clips = tracks.computeIfAbsent(track, k -> new HashSet<>());
                        for (MediaPlayer audio : clips) {
                            audio.stop();
                        }
                    }
                    Runnable handleVolume = () -> {
                        for (Map.Entry<String, HashSet<MediaPlayer>> trackList : tracks.entrySet()) {
                            for (MediaPlayer audio : trackList.getValue()) {
                                float volumeMaster = MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MASTER);
                                float volumeRecords = MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS);
                                float volume = volumeMaster * volumeRecords;
                                audio.setVolume(volume);
                            }
                        }
                    };
                    scheduledExecutor.scheduleAtFixedRate(handleVolume, 0, 1, TimeUnit.SECONDS);

                });
                socket.connect();
            });
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}