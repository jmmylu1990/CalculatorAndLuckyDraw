package com.example.amway;

import java.io.Serializable;

public class DrawResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String prizeId;
    private final String prizeName;
    private final boolean won;

    public DrawResult(String prizeId, String prizeName, boolean won) {
        this.prizeId = prizeId;
        this.prizeName = prizeName;
        this.won = won;
    }

    public String getPrizeId() {
        return prizeId;
    }

    public String getPrizeName() {
        return prizeName;
    }

    public boolean isWon() {
        return won;
    }
}
