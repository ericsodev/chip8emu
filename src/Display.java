import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class Display extends Canvas {
    public Display() {
        super(640, 320);
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setFill(Color.GREEN);
        gc.fillRect(0, 0, 640, 320);
    }
    public void draw(boolean[][] pixels) {
        assert pixels.length == 32;
        assert pixels[0].length == 64;
        GraphicsContext gc = this.getGraphicsContext2D();
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                if (pixels[y][x]) {
                    gc.setFill(Color.WHITE);
                } else {
                    gc.setFill(Color.PURPLE);
                }
                gc.fillRect(x * 10, y * 10, 10, 10);
            }
        }
    }
}
