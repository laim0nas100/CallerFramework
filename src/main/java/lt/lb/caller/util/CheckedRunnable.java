/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.caller.util;

/**
 *
 * @author Lemmin
 */
public interface CheckedRunnable extends Runnable {

    @Override
    public default void run() throws CheckedException {
        try {
            runUnsafe();
        } catch (Exception e) {
            throw new CheckedException(e);
        }
    }

    /**
     * Runs. Masks exceptions.
     *
     * @throws Exception
     */
    public void runUnsafe() throws Exception;

}
