package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;

import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

public class Timer extends Module {

    private static final CopyOnWriteArrayList<TimerRequest> requests = new CopyOnWriteArrayList<>();

    public final FloatProperty speed = new FloatProperty("speed", 1.0F, 0.01F, 10.0F);

    public Timer() {
        super("Timer", false);
    }

    public static float getRequestedSpeed() {
        float requestSpeed = requests.stream()
                .max(Comparator.comparingInt(r -> r.priority))
                .map(r -> r.speed)
                .orElse(-1f);

        if (requestSpeed > 0) return requestSpeed;

        Timer instance = Myau.moduleManager != null
                ? (Timer) Myau.moduleManager.modules.get(Timer.class)
                : null;

        if (instance != null && instance.isEnabled()) {
            return instance.speed.getValue();
        }

        return 1.0f;
    }

    public static void requestTimerSpeed(float speed, int priority, Object provider, int resetAfterTicks) {
        requests.removeIf(r -> r.provider == provider);
        requests.add(new TimerRequest(speed, priority, provider, resetAfterTicks + 1));
    }

    public static void cancelRequest(Object provider) {
        requests.removeIf(r -> r.provider == provider);
    }

    @EventTarget(Priority.HIGHEST)
    public void onLivingUpdate(LivingUpdateEvent event) {
        requests.forEach(r -> r.ticksLeft--);
        requests.removeIf(r -> r.ticksLeft <= 0);
    }

    @Override
    public void onDisabled() {
        requests.clear();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", this.speed.getValue())};
    }

    private static class TimerRequest {
        float speed;
        int priority;
        Object provider;
        int ticksLeft;

        TimerRequest(float speed, int priority, Object provider, int ticksLeft) {
            this.speed = speed;
            this.priority = priority;
            this.provider = provider;
            this.ticksLeft = ticksLeft;
        }
    }
}