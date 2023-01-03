/*
 * The MIT License
 *
 * Copyright (c) 2014, Steve Wagner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.SemanticVersioning.parsing;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.remoting.VirtualChannel;

import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.plugins.SemanticVersioning.AppVersion;
import org.jenkinsci.plugins.SemanticVersioning.InvalidBuildFileFormatException;
import org.jenkinsci.plugins.SemanticVersioning.Messages;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

@Extension
public class PomParser extends AbstractBuildDefinitionParser {

    private static final String BUILD_FILE = "pom.xml";

    public PomParser() {
	}

	@Override
	public AppVersion extractAppVersion(FilePath workspace, PrintStream logger)
			throws IOException, InvalidBuildFileFormatException {
		String version = null;

		Document document = getPom(workspace);
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression expression;
		try {
			expression = xPath.compile("/project/version");
			version = expression.evaluate(document);

		} catch (XPathExpressionException e) {
			throw new InvalidBuildFileFormatException(document.getBaseURI()
					+ " is not a valid POM file.");
		}

		if (version == null || version.length() == 0) {
			throw new InvalidBuildFileFormatException(
					"No version information found in " + document.getBaseURI());
		}
		return AppVersion.parse(version);
	}

	private Document getPom(FilePath workspace)
			throws InvalidBuildFileFormatException, IOException {

		FilePath pom;

		pom = new FilePath(workspace, BUILD_FILE);

		Document pomDocument;
		try {
			pomDocument = pom.act(new MasterToSlaveFileCallable<Document>() {
				private static final long serialVersionUID = 1L;

				public Document invoke(File pom, VirtualChannel channel)
						throws IOException, InterruptedException {

					try {
						DocumentBuilder documentBuilder;
						documentBuilder = DocumentBuilderFactory.newInstance()
								.newDocumentBuilder();
						return documentBuilder.parse(pom);

					} catch (SAXException | ParserConfigurationException e) {
						throw new InterruptedException(pom
								.getAbsolutePath()
								+ " is not a valid POM file.");
					}
				}

			});
		} catch (InterruptedException e) {
			throw new InvalidBuildFileFormatException(e.getMessage());
		}

		return pomDocument;
	}

	@SuppressWarnings("unchecked")
	public Descriptor<BuildDefinitionParser> getDescriptor() {
		return new AbstractSemanticParserDescription() {

			@Override
			public String getDisplayName() {

				return Messages.Parsers.MAVEN_POM_PARSER;
			}
		};
	}
}
