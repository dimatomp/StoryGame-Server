package ru.ifmo.ctddev.games.messages;

import ru.ifmo.ctddev.games.state.Poll;

import java.util.Map;

/**
 * Created by Aksenov239 on 28.08.2014.
 */
public class VotesInformationResponseMessage {
    private Poll[] polls;
    private UserVote[] voted;

    public VotesInformationResponseMessage() {
    }

    public VotesInformationResponseMessage(Poll[] polls, UserVote[] voted) {
        super();
        this.polls = polls;
        this.voted = voted;
    }

    public Poll[] getPolls() {
        return polls;
    }

    public void setPolls(Poll[] polls) {
        this.polls = polls;
    }

    public UserVote[] getVoted() {
        return voted;
    }

    public void setVoted(UserVote[] voted) {
        this.voted = voted;
    }
}
