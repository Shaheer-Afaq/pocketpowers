package pocketutils;

import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.function.IntConsumer;

public class TaskTicker {

    private final IntConsumer task;
    private final int tickdelay;
    private final int totalRuns;

    private int currentRun = 1;
    private int tickCounter = 0;
    private boolean finished = false;

    public TaskTicker(IntConsumer task, int tickDelay, int totalRuns) {
        this.task = task;
        this.tickdelay = tickDelay;
        this.totalRuns = totalRuns;

        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    private void onTick(MinecraftServer server1) {
        if (finished) return;

        tickCounter++;
        if (tickCounter < tickdelay) return;
        tickCounter = 0;

        if (currentRun > totalRuns) {
            finished = true;
            return;
        }
        task.accept(currentRun);
        currentRun++;
    }
}
