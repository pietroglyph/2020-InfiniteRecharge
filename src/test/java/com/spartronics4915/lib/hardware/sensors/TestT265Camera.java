package com.spartronics4915.lib.hardware.sensors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.spartronics4915.lib.math.twodim.geometry.Pose2d;
import com.spartronics4915.lib.math.twodim.geometry.Rotation2d;
import com.spartronics4915.lib.math.twodim.geometry.Twist2d;
import com.spartronics4915.lib.hardware.sensors.T265Camera.CameraUpdate;
import com.spartronics4915.lib.util.Logger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class TestT265Camera
{
    private boolean mDataRecieved = false;
    private final Object mLock = new Object();

    @Test
    public void testGeometry()
    {
        // final Pose2d origin = new Pose2d(5, -5, Rotation2d.fromDegrees(180));
        
        // final Pose2d currentPose = new Pose2d(1, 1, Rotation2d.fromDegrees(180));
        // System.out.println(origin.transformBy(currentPose));

        final Pose2d a = new Pose2d(0, 0, new Rotation2d());
        final Pose2d b = new Pose2d(1, 0, Rotation2d.fromDegrees(20));

        System.out.println(a.getTranslation().getDistance(b.getTranslation()));
        System.out.println(a.distance(b));
    }

    @Tag("hardwareDependant")
    @Test
    public void testNewCamera() throws InterruptedException
    {
        // This one is a little hard to unit test because we haven't simulated the
        // hardware
        // We mostly just make sure that we can get through this sequence without
        // throwing an exception
        // The rest is just a few sanity checks

        T265Camera cam = null;
        try
        {
            cam = new T265Camera(new Pose2d(), 0);

            // Just make sure this doesn't throw
            cam.sendOdometry(new Twist2d(0, 0, new Rotation2d()));

            cam.start((CameraUpdate update) -> {
                synchronized (mLock)
                {
                    mDataRecieved = true;
                }
                System.out.println("Got pose with confidence " + update.pose);
            });
            Logger.debug(
                "Waiting 5 seconds to recieve data... Move the camera around in the path of the shape of a cross for best results. This will not work unless you get to High confidence.");
            Thread.sleep(5000);
            cam.stop();
            synchronized (mLock)
            {
                assertTrue(mDataRecieved,
                    "No pose data was recieved after 5 seconds... Try moving the camera?");
            }

            Logger.debug("Got pose data, exporting relocalization map to java.io.tmpdir...");
            Path mapPath = Paths.get(System.getProperty("java.io.tmpdir"), "map.bin")
                .toAbsolutePath();
            cam.exportRelocalizationMap(mapPath.toString());

            if (mapPath.toFile().length() <= 0)
                fail("Relocalization map file length was 0");
            Logger.debug("Successfuly saved relocalization map, we will now try to import it");

            cam.free();

            // Try making a new camera and importing the map
            cam = new T265Camera(new Pose2d(), 0f, mapPath.toString());

            Logger.debug("Map imported without errors!");
        }
        finally
        {
            if (cam != null)
                cam.free();
        }
    }

    @Tag("hardwareDependant")
    @Test
    public void testErrorChecking()
    {
        T265Camera cam = null;
        try
        {
            cam = new T265Camera(new Pose2d(), 0);
            cam.start((CameraUpdate unused) -> {});

            final T265Camera camTemp = cam;
            assertThrows(RuntimeException.class, () -> camTemp.start((CameraUpdate unused) -> {}));
        }
        finally
        {
            if (cam != null)
                cam.free();
        }
    }

}
