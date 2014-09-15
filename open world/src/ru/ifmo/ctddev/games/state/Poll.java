package ru.ifmo.ctddev.games.state;

/**
 * Created by Aksenov239 on 30.08.2014.
 */
public class Poll {
    private long id;
    private String question;
    private String[] options;
    private int[] minimalAmount;
    private int priority = 0;
    private Integer result;
    private String date;
    private int[] investedMoney;

    public Poll() {
    }

    public Poll(long id, String question, String[] options, int[] minimalAmount) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.minimalAmount = minimalAmount;
    }

    public void vote(int option, int amount) {
        investedMoney[option] += amount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public int[] getMinimalAmount() {
        return minimalAmount;
    }

    public void setMinimalAmount(int[] minimalAmount) {
        this.minimalAmount = minimalAmount;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
