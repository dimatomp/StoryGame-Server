package ru.ifmo.ctddev.games.state;

import ru.ifmo.ctddev.games.messages.UserVote;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by Aksenov239 on 30.08.2014.
 */
public class VotesState {
    private static Map<Long, Poll> votes;
    private static Map<Long, Poll> archived;
    private static long id = 1;

    // TODO: There obviously should be synchronization

    public VotesState() {
        votes = new HashMap<Long, Poll>();
        archived = new HashMap<Long, Poll>();
    }

    public void addPoll(String question, int[] money, String... options) {
        Poll vote = new Poll(id, question, options, money);
        votes.put(id++, vote);
    }

    public static Poll[] getVotes() {
        Poll[] result = new Poll[votes.size()];
        int l = 0;
        for (Poll vote : votes.values()) {
            result[l++] = vote;
        }
        return result;
    }

    public static boolean vote(PlayerState state, long id, String optionName, int amount) {
        Map<Long, UserVote> player_votes = state.getVotes();
        Poll poll = votes.get(id);
        int option = 0;
        while (!poll.getOptions()[option].equals(optionName))
            option++;
        if (player_votes.containsKey(id)) {
            UserVote already = player_votes.get(id);
            if (amount <= already.getMoney())
                return false;
        }
        player_votes.put(id, new UserVote(option, amount));
        poll.vote(option, amount);
        return true;
    }

    public static void setArchived(Poll v) {
        votes.remove(v.getId());
        archived.put(v.getId(), v);
    }

    public boolean active(Long id) {
        return votes.containsKey(id);
    }

}
