/* Copyright (C) 2005 Vladimir Roubtsov. All rights reserved.
 */
package com.vladium.util.optimize;

// ----------------------------------------------------------------------------
/**
 * @author Vlad Roubtsov
 */
public
interface IFunctionGradient
{
    void evaluate (double [] x, double [] out);
    
} // end of interface
// ----------------------------------------------------------------------------
