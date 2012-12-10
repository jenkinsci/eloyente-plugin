/*
 * Copyright 2012 pardogonzalezj.
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

import org.kohsuke.stapler.DataBoundConstructor;
import javax.xml.xpath.XPathExpressionException;

/**
 *
 * @author pardogonzalezj
 */
public class Variable {

    public String envName;
    public XPathExpressionHandler envExpr;

    @DataBoundConstructor
    public Variable(String envName, String envExpr) throws XPathExpressionException {
        this.envName = envName;
        this.envExpr = new XPathExpressionHandler(envExpr);
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvExpr(String envExpr)  throws XPathExpressionException {
        this.envExpr.setExpression(envExpr);
    }

    public String getEnvExpr() {
        return envExpr.getExpression();
    }

    public String resolve(String xml) throws XPathExpressionException {
try {
System.out.println("evaluate expression "+envExpr.getExpression()+" against "+xml);
System.out.println("boolean value: "+envExpr.test(xml));
System.out.println("string value: "+envExpr.evaluate(xml));
} catch (XPathExpressionException e) { e.printStackTrace(); throw e; }
  catch (Exception e) { e.printStackTrace(); }
       return envExpr.evaluate(xml);
    }

}
