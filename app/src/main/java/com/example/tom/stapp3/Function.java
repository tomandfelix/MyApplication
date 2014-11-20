package com.example.tom.stapp3;

/**
 * Created by Tom on 5/11/2014.
 * implements a function that can be called with a parameter of type INPUT
 */

/**
 * executes the provided function
 * @param <INPUT> Input type of the function
 */
public interface Function<INPUT> {
    /**
     *
     * @param param input of type INPUT to the call function
     */
    void call(INPUT param);
}