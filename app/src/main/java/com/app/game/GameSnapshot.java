package com.app.game;

import java.io.Serializable;

public class GameSnapshot implements Serializable {
    public float playerX, playerY;
    public float cameraX;
    public int playerLastDirection;
}
