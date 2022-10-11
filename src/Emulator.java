import javafx.scene.input.KeyCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static java.util.Map.entry;

public class Emulator {
    private final int[] memory;
    private final int[] stack;
    private final int[] reg;
    private int pc;
    private int i_reg;
    private byte stack_ptr;
    private final boolean[][] displayBuffer;
    private int delayTimer;
    private int soundTimer;
    private HashMap<Integer, Boolean> activeKeys;

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

    private HashMap<Integer, Integer> keycodeHexMap = new HashMap<>(Map.ofEntries(
            entry(KeyCode.DIGIT1.getCode(), 0x1),
            entry(KeyCode.DIGIT2.getCode(), 0x2),
            entry(KeyCode.DIGIT3.getCode(), 0x3),
            entry(KeyCode.DIGIT4.getCode(), 0xC),
            entry(KeyCode.Q.getCode(), 0x4),
            entry(KeyCode.W.getCode(), 0x5),
            entry(KeyCode.E.getCode(), 0x6),
            entry(KeyCode.R.getCode(), 0xD),
            entry(KeyCode.A.getCode(), 0x7),
            entry(KeyCode.S.getCode(), 0x8),
            entry(KeyCode.D.getCode(), 0x9),
            entry(KeyCode.F.getCode(), 0xE),
            entry(KeyCode.Z.getCode(), 0xA),
            entry(KeyCode.X.getCode(), 0x0),
            entry(KeyCode.C.getCode(), 0xB),
            entry(KeyCode.V.getCode(), 0xF)
        ));


    public Emulator(HashMap<Integer, Boolean> activeKeys) {
        this.memory = new int[4096];
        this.stack = new int[16];
        this.reg = new int[16];
        this.pc = 512;
        this.stack_ptr = 0;
        this.displayBuffer = new boolean[32][64];
        this.activeKeys = activeKeys;

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
//        System.out.println("PC: " + pc + " Instr: " + String.format("0x%04X", instruction));
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
                        this.stack_ptr--;
                        this.pc = this.stack[this.stack_ptr];
                        break;
                    default:
                        System.out.println("Unknown Instruction: " + instruction);
//                    default:
//                        // Sys Addr, 0NNN
//                        break;

                }
                break;
            case 0x1000:
                // Jump, 1NNN
                this.pc = 0x0FFF & instruction; // 554  ends up setting pc to 552 again, inc to 554 then ...
                break;
            case 0x2000:
                // Push PC to stack, set pc to NNN, 2NNN
                this.stack[stack_ptr++] = pc;
                this.pc = 0x0FFF & instruction;
                break;
            case 0x3000:
                // Skip next instruction if VX == NN, 3XNN
                x = (0x0F00 & instruction) >> 8;
                if (this.reg[x] == (0x00FF & instruction)) {
                    pc += 2;
                }
                break;
            case 0x4000:
                // Skip next instruction if VX != NN, 4XNN
                x = (0x0F00 & instruction) >> 8;
                if (this.reg[x] != (0x00FF & instruction)) {
                    pc += 2;
                }
                break;
            case 0x5000:
                // Skip next instruction if VX == VY, 5XY0
                x = (0x0F00 & instruction) >> 8;
                y = (0x00F0 & instruction) >> 4;
                if (this.reg[x] == this.reg[y]) {
                    pc += 2;
                }
                break;
            case 0x6000:
                // Set register VX, 6XNN
                this.reg[(0x0F00 & instruction) >> 8] = 0x00FF & instruction;
                break;
            case 0x7000:
                // Add register VX, 7XNN
                x = (0x0F00 & instruction) >> 8;
                this.reg[x] = (this.reg[x] + (0x00FF & instruction)) &0xFF;
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
                        this.reg[0xF]= 0;
                        if (reg[x] + reg[y] > 0xFF) {
                            this.reg[0xF]= 1;
                        }
                        this.reg[x] = (this.reg[x] + this.reg[y]) & 0xFF;
                        break;
                    case 0x0005:
                        // Set VX to (VX - VY) and set VF to carry, 8XY5
                        this.reg[0xF] = 1;
                        if (this.reg[y] > this.reg[x]) {
                            this.reg[0xF] = 0;
                        }
                        this.reg[x] = (this.reg[x] - this.reg[y]) &0xFF;
                        break;
                    case 0x0007:
                        // Set VX to (VY - VX) and set VF to carry, 8XY5
                        this.reg[0xF] = 1;
                        if (this.reg[x] > this.reg[y]) {
                            this.reg[0xF] = 0;
                        }
                        this.reg[x] = (this.reg[y] - this.reg[x]) & 0xFF;
                        break;
                    case 0x0006:
                        // Set VX to (VY >> 1) and set VF to the shifted bit, 8XY6
                        // Uses original COSMAC VIP instruction
//                        this.reg[0xF] = this.reg[y] & 0x1;
//                        this.reg[x] = this.reg[y] >> 1;
                        // Super Chip Instruction
                        this.reg[0xF] = this.reg[x] & 0x1;
                        this.reg[x] >>= 1;
                        break;
                    case 0x000E:
                        // Set VX to (VY << 1) and set VF to the shifted bit, 8XYE
                        // Uses original COSMAC VIP instruction
//                        this.reg[0xF] = (this.reg[x] & 0x80) >> 7;
//                        this.reg[x] = (this.reg[y] << 1) & 0xFF;
                        // Super Chip Instruction
                        this.reg[0xF] = this.reg[x] >> 7;
                        this.reg[x] <<= 1;
                        break;
                    default:
                        System.out.println("Unknown Instruction: " + instruction);
                }
                break;
            case 0x9000:
                // Skip next instruction if VX != VY, 9XY0
                x = (0x0F00 & instruction) >> 8;
                y = (0x00F0 & instruction) >> 4;
                if (this.reg[x] != this.reg[y]) {
                    this.pc += 2;
                }
                break;
            case 0xA000:
                // Set Reg I, ANNN
                this.i_reg = 0x0FFF & instruction;
                break;
            case 0xB000:
                // Jump to NNN + V0, BNNN
                this.pc = (0x0FFF & instruction) + this.reg[0];
                break;
            case 0xC000:
                // Set VX to RANDNUM & 0xNN, 0xCXNN
                x = (0x0F00 & instruction) >> 8;
                Random rand = new Random();
                this.reg[x] = rand.nextInt(0, 255) & (0x00FF & instruction);
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
                break;
            case 0xE000:
                x = (0x0F00 & instruction) >> 8;
                int keycode = -1;
                for (Map.Entry<Integer, Integer> entry : keycodeHexMap.entrySet()) {
                    if (entry.getValue().equals(this.reg[x])) keycode = entry.getKey();
                }
                switch (0x00FF & instruction) {
                    case 0x009E:
                        // Skip next instruction if key in VX is being pressed, EX9E
                        if (activeKeys.containsKey(keycode)) {
                            pc += 2;
                        }
                        break;
                    case 0x00A1:
                        // Skip next instruction if key in VX is not pressed, EXA1
                        if (!activeKeys.containsKey(keycode)) {
                            pc += 2;
                        }
                        break;
                    default:
                        System.out.println("Unknown Instruction: " + instruction);
                }
                break;
            case 0xF000:
                x = (0x0F00 & instruction) >> 8;
                switch (0x00FF & instruction) {
                    case 0x0007:
                        // Set VX to delay timer, FX07
                        this.reg[x] = this.delayTimer;
                        break;
                    case 0x0015:
                        // Set delay timer to VX, FX15
                        this.delayTimer = this.reg[x];
                        break;
                    case 0x0018:
                        // Set sound timer to VX, FX18
                        this.soundTimer = this.reg[x];
                        break;
                    case 0x001E:
                        // Set I to VX + I, use VF as carry, FX1E
                        // Assume I is a 12 bit register, memory addresses only go up to 4095.
                        this.reg[0xF] = 0;
                        if (i_reg + reg[x] > 0xFFF) {
                            this.reg[0xF] = 1;
                        }
                        this.i_reg = (this.reg[x] + this.i_reg) & 0xFFF;
                        break;
                    case 0x000A:
                        // Block until a key is pressed, store key into VX, FX0A
                        int key = -1;
                        for (int i = 0; i < 50; i++) {
                            if (activeKeys.isEmpty()) continue;
                            Set<Integer> activeSet = new HashSet<Integer>(activeKeys.keySet());
                            activeSet.retainAll(keycodeHexMap.keySet());
                            if (activeSet.isEmpty()) continue;
                            key = keycodeHexMap.getOrDefault(activeSet.toArray()[0], -1);
                            System.out.println(key);
                            break;
                        }

                        if (key == -1) {
                            pc -= 2; // Repeat this instruction
                        }
                        this.reg[x] = key;

                        break;
                    case 0x0029:
                        // Set I to the memory address of the hex character in VX, FX29
                        // Each 4x5 font is stored in order starting from 0x0.
                        this.i_reg = this.reg[x] * 0x5;
                        break;
                    case 0x0033:
                        // Decimal conversion, FX33
                        // Store third digit in mem[I], second in mem[I + 1], first in mem[I + 2]
                        this.memory[this.i_reg + 2] = this.reg[x]  % 10;
                        this.memory[this.i_reg + 1] = (this.reg[x]  / 10) % 10;
                        this.memory[this.i_reg] = this.reg[x]  / 100;
                        break;
                    case 0x0055:
                        // Save V0 to VX to memory at I to I + X, FX55
                        System.arraycopy(this.reg, 0, this.memory, this.i_reg, x + 1);
//                        this.i_reg += x + 1; // Used for original interpreter
                        break;
                    case 0x0065:
                        // Load memory from I to I + X to V0 to VX, FX65
                        System.arraycopy(this.memory, this.i_reg, this.reg, 0, x + 1);
//                        this.i_reg += x + 1; // Used for original interpreter
                        break;
                    default:
                        System.out.println("Unknown Instruction: " + instruction);
                }
                break;
            default:
                System.out.println("Unknown Instruction: " + instruction);

        }
    }
    void decrementDelayTimer() {
        this.delayTimer--;
    }

    void decrementSoundTimer() {
        this.soundTimer--;
    }

    int getDelayTimer() {
        return this.delayTimer;
    }

    int getSoundTimer() {
        return this.soundTimer;
    }
}
