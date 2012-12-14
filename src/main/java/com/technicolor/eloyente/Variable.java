/*
 * Copyright 2012 Technicolor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.technicolor.eloyente;

import javax.xml.xpath.XPathExpressionException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Juan Luis Pardo Gonz&aacute;lez
 * @author Isabel Fern&aacute;ndez D&iacute;az
 */
public class Variable {

    /**
     * Name of the environment variable.
     */
    public String envName;
    /**
     * Value of the environment variable.
     */
    public XPathExpressionHandler envExpr;

    /**
     * Constructor for an environment variables.
     *
     *
     * @param envName
     * @param envExpr
     * @throws XPathExpressionException
     */
    @DataBoundConstructor
    public Variable(String envName, String envExpr) throws XPathExpressionException {
        this.envName = envName;
        this.envExpr = new XPathExpressionHandler(envExpr);
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    /**
     * This method returns the name of the environment variable.
     *
     * config.jelly calls this method to obtain the value of the field Name for
     * a variable.
     *
     */
    public String getEnvName() {
        return envName;
    }

    public void setEnvExpr(String envExpr) throws XPathExpressionException {
        this.envExpr.setExpression(envExpr);
    }

    /**
     * This method returns the value of the environment variable.
     *
     * config.jelly calls this method to obtain the value of the field Value for
     * a variable.
     *
     */
    public String getEnvExpr() {
        return envExpr.getExpression();
    }

    /**
     * Returns the result of evaluating the expression against the provided XML
     * document.
     *
     * @param xml
     * @throws XPathExpressionException
     */
    public String resolve(String xml) throws XPathExpressionException {
        return envExpr.evaluate(xml);
    }
}
