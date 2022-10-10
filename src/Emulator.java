import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Emulator {
    private int[] memory;
    private int[] stack;
    private int[] reg;
    private int pc;
    private int i_reg;
    private byte stack_ptr;
    private boolean[][] displayBuffer;
    private boolean testFlag = false;

    private final char[] FONTSET =
    {
            0xF0, 0x90, 0x90, 0x90, 0xF0, //0
            0x20, 0x60, 0x20, 0x20, 0x70, //1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, //2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, //3
            0x90, 0x90, 0xF0, 0x10, 0x10, //4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, //5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, //6
            0xF0, 0x10, 0x20, 0x40, 0x40, //7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, //8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, //9
            0xF0, 0x90, 0xF0, 0x90, 0x90, //A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, //B
            0xF0, 0x80, 0x80, 0x80, 0xF0, //C
            0xE0, 0x90, 0x90, 0x90, 0xE0, //D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, //E
            0xF0, 0x80, 0xF0, 0x80, 0x80  //F
};


    public Emulator() {
        this.memory = new int[4096];
        this.stack = new int[16];
        this.reg = new int[16];
        this.pc = 512;
        this.stack_ptr = 0;
        this.displayBuffer = new boolean[32][64];

        for (int i = 0; i < 80; i++) {
            this.memory[i] = this.FONTSET[i];
        }
    }

    void loadFile(String file) throws IOException {
        FileInputStream fs = new FileInputStream(new File(file));
        int b;
        int i = 0;
        while ((b = fs.read()) != -1) {
            this.memory[512 + i] = b;
            i++;
        }
        fs.close();
    }

    boolean[][] getDisplayBuffer() {
        return this.displayBuffer;
    }

    void cycle() {
        int instruction = (this.memory[pc] & 0xFF) << 8 | (this.memory[pc+1] & 0xFF);
        this.pc += 2;
        int x, y;

        switch (instruction & 0xF000) {
            // Extract first 4 bits
            case 0x0000:
                switch (instruction & 0x0FF) {
                    case 0x00E0:
                        // CLS, 00E0
                        for (int i = 0; i < 32; i++) {
                            for(int j = 0; j < 64; j++) {
                                this.displayBuffer[i][j] = false;
                            }
                        }
                        break;
                    case 0x00EE:
                        // Return, 00EE
                        break;
                    default:
                        // Sys Addr, 0NNN
                        break;

                }
                break;
            case 0x1000:
                // Jump, 1NNN
                this.pc = 0x0FFF & instruction; // 554  ends up setting pc to 552 again, inc to 554 then ...
                break;
            case 0x2000:
                break;
            case 0x3000:
                break;
            case 0x4000:
                break;
            case 0x5000:
                break;
            case 0x6000:
                // Set register VX, 6XNN
                this.reg[(0x0F00 & instruction) >> 8] = 0x00FF & instruction;
                break;
            case 0x7000:
                // Add register VX, 7XNN
                this.reg[(0x0F00 & instruction) >> 8] += 0x00FF & instruction;
                break;
            case 0x8000:
                x = (0x0F00 & instruction) >> 8;
                y = (0x00F0 & instruction) >> 4;
                switch (0x000F & instruction) {
                    case 0x0000:
                        // Set VX to VY, 8XY0
                        this.reg[x] = this.reg[y];
                        break;
                    case 0x0001:
                        // Set VX to (VX or VY), 8XY1
                        this.reg[x] |= this.reg[y];
                        break;
                    case 0x0002:
                        // Set VX to (VX and VY), 8XY2
                        this.reg[x] &= this.reg[y];
                        break;
                    case 0x0003:
                        // Set VX to (VX xor VY), 8XY3
                        this.reg[x] ^= this.reg[y];
                        break;
                    case 0x0004:
                        // Set VX to (VX + VY) and set VF to carry, 8XY4
                        this.reg[x] += this.reg[y];
                        this.reg[0xF] = (byte) (this.reg[x] / 128);
                        this.reg[x] %= 128;
                        break;
                    case 0x0005:
                        // Set VX to (VX - VY) and set VF to carry, 8XY5
                        this.reg[0xF] = 1;
                        if (this.reg[y] > this.reg[x]) {
                            this.reg[0xF] = 0;
                        }
                        this.reg[x] = this.reg[x] - this.reg[y];
                        break;
                    case 0x0007:
                        // Set VX to (VY - VX) and set VF to carry, 8XY5
                        this.reg[0xF] = 1;
                        if (this.reg[x] > this.reg[y]) {
                            this.reg[0xF] = 0;
                        }
                        this.reg[x] = this.reg[y] - this.reg[x];
                        break;
                }
                break;
            case 0x9000:
                break;
            case 0xA000:
                // Set Reg I, ANNN
                this.i_reg = 0x0FFF & instruction;
                break;
            case 0xB000:
                break;
            case 0xC000:
                break;
            case 0xD000:
                // Draw, DXYN
                this.reg[0xF] = 0;
                x = (this.reg[(0x0F00 & instruction) >> 8] % 64);
                y = (this.reg[(0x00F0 & instruction) >> 4] % 32);
                byte n = (byte) (0x000F & instruction);
                for (int i = 0; i < n; i++) {
                    if (y + i > 31) break;
                    int pixels = this.memory[i_reg + i];
                    for (int bit = 0; bit < 8; bit++) {
                        if (x + bit > 63) break;

                        if ((pixels & (0x80 >> bit)) != 0) {
                            boolean currentPixel = this.displayBuffer[y + i][x + bit];

                            if (currentPixel) {
                                this.displayBuffer[y + i][x + bit] = false;
                                this.reg[0xF] = 1;
                            } else {
                                this.displayBuffer[y + i][x + bit] = true;
                            }
                        }
                    }
                }
                testFlag = true;
                break;
            case 0xE000:
                break;
            case 0xF000:
                break;

        }
    }
    boolean getTestFlag() {
        return this.testFlag;
    }

}
