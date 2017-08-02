/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.horako.stemmer.run;

/**
 *
 * @author Ondrej Horak &lt;ondrej.horak@centrum.cz&gt;
 */
public interface IRunner {
    public void init(String[] args) throws Exception;
    public void run() throws Exception;
}
