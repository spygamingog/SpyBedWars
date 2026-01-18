package com.spygamingog.spybedwars.api.arena;

public enum ArenaType {
    SOLO(1),
    DOUBLES(2),
    TRIPLES(3),
    FOURS(4);

    private final int playersPerTeam;

    ArenaType(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public int getTeamSize() {
        return playersPerTeam;
    }
}
