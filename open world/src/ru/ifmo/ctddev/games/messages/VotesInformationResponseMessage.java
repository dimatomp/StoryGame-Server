package ru.ifmo.ctddev.games.messages;

import ru.ifmo.ctddev.games.state.Poll;

import java.util.Map;

/**
 * Created by Aksenov239 on 28.08.2014.
 */
public class VotesInformationResponseMessage {
    private Poll[] polls;
    private Map <Integer, UserVote> voted;

    public VotesInformationResponseMessage() {
    }

    public VotesInformationResponseMessage(Poll[] polls, Map <Integer, UserVote> voted) {
        this.polls = polls;
        this.voted = voted;
        System.err.println("send votes infomration response message ");
/*        for (int i = 0; i < polls.length; ++i) {
            System.err.println(polls[i].getId());
            for (int j = 0; j < polls[i].getOptionsId().length; ++j)
                System.err.print(polls[i].getOptionsId()[j] + " " + polls[i].getOptionsName()[j] + " " + polls[i].getMinimalAmount()[j] + "; ");
            System.err.println("");
        }*/
    }

    public Poll[] getPolls() {
        return polls;
    }

    public void setPolls(Poll[] polls) {
        this.polls = polls;
    }

    public Map <Integer, UserVote> getVoted() {
        return voted;
    }

    public void setVoted(Map <Integer, UserVote> voted) {
        this.voted = voted;
    }
}
