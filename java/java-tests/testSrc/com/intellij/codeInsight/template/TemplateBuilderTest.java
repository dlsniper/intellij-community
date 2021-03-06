package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

public class TemplateBuilderTest extends LightCodeInsightFixtureTestCase {
  public void testRunInlineTemplate() throws Throwable {
    TemplateManagerImpl.setTemplateTesting(myFixture.getProject(), getTestRootDisposable());
    myFixture.configureByText("a.java", "class A {\n" +
                                        "  public String tes<caret>t() {\n" +
                                        "  }\n" +
                                        "}");
    new WriteCommandAction.Simple<Void>(myFixture.getProject(), myFixture.getFile()) {
      @Override
      protected void run() throws Throwable {
        TemplateBuilderFactory.getInstance().createTemplateBuilder(myFixture.getElementAtCaret())
          .run(myFixture.getEditor(), true);
      }
    }.execute();
    myFixture.checkResult("class A {\n" +
                          "  <caret>public String test() {\n" +
                          "  }\n" +
                          "}");
  }
}
