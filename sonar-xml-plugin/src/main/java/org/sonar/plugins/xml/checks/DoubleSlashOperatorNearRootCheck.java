/*
 * SonarQube XML Plugin
 * Copyright (C) 2010-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.xml.checks;

import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Perform check for
 */
@Rule(key = "DoubleSlashOperatorCheck", name = "Double slash operator near root should not be used", priority = Priority.MINOR, tags = {
        "convention" })
@BelongsToProfile(title = CheckRepository.SONAR_WAY_PROFILE_NAME, priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class DoubleSlashOperatorNearRootCheck extends AbstractXmlCheck {

    @RuleProperty(key = "markAll", description = "Mark all tab errors", defaultValue = "false")
    private boolean markAll;
    private boolean validationReady;
    
    @Override
    public void validate(XmlSourceCode xmlSourceCode) {
        setWebSourceCode(xmlSourceCode);

        validationReady = false;
        Document document = getWebSourceCode().getDocument(false);
        Element documentElement = document.getDocumentElement();
        if (documentElement != null) {
            findDoubleSlashOperatorNearRoot(documentElement);
        }
    }

    /**
     * Find Illegal tabs in whitespace.
     */
    private void findDoubleSlashOperatorNearRoot(Node node) {

        // check whitespace in the node
        for (Node sibling = node.getPreviousSibling(); sibling != null; sibling = sibling.getPreviousSibling()) {
            if (sibling.getNodeType() == Node.TEXT_NODE) {
                String text = sibling.getTextContent();
                
                boolean testContext = testContext(text);
                if (testContext) {
                    createNewViolation(getWebSourceCode().getLineForNode(sibling));
                    // one violation for this node is enough
                    break;
                }
            }
        }

        // check the child elements of the node
        for (Node child = node.getFirstChild(); !validationReady && child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                findDoubleSlashOperatorNearRoot(child);
            }
        }
    }

    private boolean testContext(String xml) {
        
        String xpath = "//(@match | @select)[starts-with(., '//')]";
        String evaluate = null;
        try {
             evaluate = XPathFactory.newInstance().newXPath().evaluate(xpath, new InputSource(new StringReader(xml)));
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        boolean test = "".equalsIgnoreCase(evaluate);
        return test;
    }

    private void createNewViolation(int lineNumber) {
        if (!markAll) {
            createViolation(lineNumber,
                    "Avoid using the operator // near the root of a large tree (this is the first occurrence)");
            validationReady = true;
        } else {
            createViolation(lineNumber, "Detect double slash operator near root in your XML files.");
        }
    }

    public boolean isMarkAll() {
        return markAll;
    }

    public void setMarkAll(boolean markAll) {
        this.markAll = markAll;
    }
}
