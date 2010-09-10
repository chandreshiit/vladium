/* Copyright (C) 2005 Vladimir Roubtsov. All rights reserved.
 */
package com.vladium.util.optimize;

// ----------------------------------------------------------------------------
/**
 * @author Vlad Roubtsov, (C) 2005
 */
public
interface IDifferentiableFunction extends IFunction
{
    IFunctionGradient gradient ();
    
} // end of interface
// ----------------------------------------------------------------------------