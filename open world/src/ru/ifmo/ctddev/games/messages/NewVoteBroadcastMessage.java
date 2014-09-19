package ru.ifmo.ctddev.games.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.ifmo.ctddev.games.state.Poll;

/**
 * Created by pva701 on 9/18/14.
 */
public class NewVoteBroadcastMessage {
    private int id;
    private String question;
    private String[] optionsName;

    private int[] minimalAmount;
    private int priority = 0;
    private Integer result;
    private String date;
    private int[] investedMoney;

    public NewVoteBroadcastMessage() {}
    public NewVoteBroadcastMessage(Poll poll) {
        id = poll.getId();
        question = poll.getQuestion();
        optionsName = poll.getOptionsName();
        minimalAmount = poll.getMinimalAmount();
        result = poll.getResult();
        date = poll.getDate();
        investedMoney = poll.getInvestedMoney();
    }

    public int getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String[] getOptionsName() {
        return optionsName;
    }

    public int[] getMinimalAmount() {
        return minimalAmount;
    }

    public int getPriority() {
        return priority;
    }

    public Integer getResult() {
        return result;
    }

    public String getDate() {
        return date;
    }

    public int[] getInvestedMoney() {
        return investedMoney;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setOptionsName(String[] optionsName) {
        this.optionsName = optionsName;
    }

    public void setMinimalAmount(int[] minimalAmount) {
        this.minimalAmount = minimalAmount;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setInvestedMoney(int[] investedMoney) {
        this.investedMoney = investedMoney;
    }
}
