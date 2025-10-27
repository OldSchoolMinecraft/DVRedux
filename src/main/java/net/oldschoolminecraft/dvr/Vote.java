package net.oldschoolminecraft.dvr;

import java.util.*;

public class Vote
{
    private final VoteType type;
    private final Set<String> votedPlayers = Collections.synchronizedSet(new HashSet<String>());
    private int yesCount = 0;
    private int noCount = 0;

    public Vote(VoteType type)
    {
        this.type = type;
    }

    public VoteType getType()
    {
        return type;
    }

    public boolean castVote(String playerName, boolean yes)
    {
        synchronized (votedPlayers)
        {
            if (votedPlayers.contains(playerName)) return false;
            votedPlayers.add(playerName);
            if (yes) yesCount++;
            else noCount++;
            return true;
        }
    }

    public int getYesVotes()
    {
        synchronized (votedPlayers)
        {
            return yesCount;
        }
    }

    public int getNoVotes()
    {
        synchronized (votedPlayers)
        {
            return noCount;
        }
    }

    public int getTotalVotes()
    {
        synchronized (votedPlayers)
        {
            return yesCount + noCount;
        }
    }
}