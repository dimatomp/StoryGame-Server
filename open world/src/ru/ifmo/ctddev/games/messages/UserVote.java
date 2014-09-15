package ru.ifmo.ctddev.games.messages;

/**
 * Created by dimatomp on 14.09.14.
 */
public class UserVote {
    private int option;
    private int money;

    public UserVote(int option, int money) {
        this.option = option;
        this.money = money;
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }
}
