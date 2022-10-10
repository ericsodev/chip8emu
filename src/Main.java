import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
    private ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(2);
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Display display = new Display();
        Group group = new Group(display);
        Scene scene = new Scene(group, 640, 320);
        Emulator cpu = new Emulator();
        cpu.loadFile("ibm.ch8");
        ScheduledFuture<?> cpuThread = pool.scheduleWithFixedDelay(cpu::cycle, 0, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> displayThread = pool.scheduleWithFixedDelay(() -> {
            display.draw(cpu.getDisplayBuffer());
        }, 0, 17, TimeUnit.MILLISECONDS);
        stage.setScene(scene);
        stage.show();
//        while (true) {
//            cpu.cycle();
//            display.draw(cpu.getDisplayBuffer());
//            boolean[][] x = cpu.getDisplayBuffer();
//            int count = 0;
//            for (int i =0; i < 32; i++) {
//                for (int j=0; j<64; j++) {
//                    if (x[i][j]) count++;
//                }
//            }
//            System.out.println(count);
//        }



    }
    @Override
    public void stop() {
        pool.shutdown();
    }
}