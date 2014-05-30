/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package common.configuration;

/**
 *  A set of gradient types...
 * @author Andreas
 */
public class GradientType {
    /**
     * Order nodes by free CPUs.
     */
    public static final int TYPE_CPU = 0;
    
    /**
     * Order nodes by free memory.
     */
    public static final int TYPE_MEMORY = 1;
    
    /**
     * Order nodes by CPU and Memory equally.
     */
    public static final int TYPE_COMBINED = 2;
}
