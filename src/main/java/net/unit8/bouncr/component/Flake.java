package net.unit8.bouncr.component;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import enkan.exception.MisconfigurationException;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.*;

public class Flake extends SystemComponent {
    private Clock clock;
    private int sequence;
    private long lastTime;
    private byte[] macAddress;

    private byte[] getMacAddress() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces())
                    .stream()
                    .filter(ni -> {
                        try {
                            return !ni.isLoopback() && ni.isUp();
                        } catch (SocketException e) {
                            throw new MisconfigurationException("", e);
                        }
                    })
                    .map(ni -> {
                        try {
                            return ni.getHardwareAddress();
                        } catch (SocketException e) {
                            throw new MisconfigurationException("", e);
                        }
                    })
                    .findAny()
                    .orElseThrow(() -> new MisconfigurationException(""));
        } catch (SocketException e) {
            throw new MisconfigurationException("", e);
        }
    }

    public synchronized BigInteger generateId() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        long current = clock.millis();
        if (current != lastTime) {
            lastTime = current;
            sequence = 0;
        } else {
            sequence++;
        }
        return new BigInteger(buf.putLong(lastTime).put(macAddress).putShort((short)sequence)
                .array());
    }

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<Flake>() {
            @Override
            public void start(Flake component) {
                clock = Clock.systemUTC();
                lastTime = clock.millis();
                macAddress = getMacAddress();
                sequence = 0;
            }

            @Override
            public void stop(Flake component) {
                lastTime = clock.millis();
                clock = null;
                macAddress = null;
                sequence = 0;
            }
        };
    }
}
