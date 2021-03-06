/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.refactoring;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.ProjectScope;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SystemProperties;
import com.jetbrains.python.PythonTestUtil;
import com.jetbrains.python.codeInsight.PyCodeInsightSettings;
import com.jetbrains.python.fixtures.PyTestCase;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyTargetExpression;
import com.jetbrains.python.psi.stubs.PyClassNameIndex;
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex;
import com.jetbrains.python.psi.stubs.PyVariableNameIndex;
import com.jetbrains.python.refactoring.move.PyMoveModuleMembersProcessor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;

import static com.jetbrains.python.refactoring.move.PyMoveModuleMemberUtil.isMovableModuleMember;

/**
 * @author vlan
 */
public class PyMoveTest extends PyTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SystemProperties.setTestUserName("user1");
  }

  public void testFunction() {
    doMoveSymbolTest("f", "b.py");
  }

  public void testClass() {
    doMoveSymbolTest("C", "b.py");
  }

  // PY-11923
  public void testTopLevelVariable() {
    doMoveSymbolTest("Y", "b.py");
  }

  // PY-11923
  public void testMovableTopLevelAssignmentDetection() {
    runWithLanguageLevel(LanguageLevel.PYTHON30, new Runnable() {
      public void run() {
        myFixture.configureByFile("/refactoring/move/" + getTestName(true) + ".py");
        assertFalse(isMovableModuleMember(findFirstNamedElement("X1")));
        assertFalse(isMovableModuleMember(findFirstNamedElement("X3")));
        assertFalse(isMovableModuleMember(findFirstNamedElement("X2")));
        assertFalse(isMovableModuleMember(findFirstNamedElement("X4")));
        assertFalse(isMovableModuleMember(findFirstNamedElement("X5")));
        assertFalse(isMovableModuleMember(findFirstNamedElement("X6")));
        assertFalse(isMovableModuleMember(findFirstNamedElement("X7")));
        assertTrue(isMovableModuleMember(findFirstNamedElement("X8")));
      }
    });
  }

  // PY-3929
  // PY-4095
  public void testImportAs() {
    doMoveSymbolTest("f", "b.py");
  }

  // PY-3929
  public void testQualifiedImport() {
    doMoveSymbolTest("f", "b.py");
  }

  // PY-4074
  public void testNewModule() {
    doMoveSymbolTest("f", "b.py");
  }

  // PY-4098
  public void testPackageImport() {
    doMoveSymbolTest("f", "b.py");
  }

  // PY-4130
  // PY-4131
  public void testDocstringTypes() {
    doMoveSymbolTest("C", "b.py");
  }

  // PY-4182
  public void testInnerImports() {
    doMoveSymbolTest("f", "b.py");
  }

  // PY-5489
  public void testImportSlash() {
    doMoveSymbolTest("function_2", "file2.py");
  }

  // PY-5489
  public void testImportFirstWithSlash() {
    doMoveSymbolTest("function_1", "file2.py");
  }

  // PY-4545
  public void testBaseClass() {
    doMoveSymbolTest("B", "b.py");
  }

  // PY-4379
  public void testModule() {
    doMoveFileTest("p1/p2/m1.py", "p1");
  }

  // PY-5168
  public void testModuleToNonPackage() {
    doMoveFileTest("p1/p2/m1.py", "nonp3");
  }

  // PY-6432
  public void testStarImportWithUsages() {
    doMoveSymbolTest("f", "c.py");
  }

  // PY-6447
  public void testFunctionToUsage() {
    doMoveSymbolTest("f", "b.py");
  }

  // PY-5850
  public void testSubModuleUsage() {
    doMoveSymbolTest("f", "b.py");
  }

  // PY-6465
  public void testUsageFromFunction() {
    doMoveSymbolTest("use_f", "b.py");
  }

  // PY-6571
  public void testStarImportUsage() {
    doMoveSymbolTest("g", "c.py");
  }

  // PY-13870
  public void testConditionalImport() {
    doMoveFileTest("mod2.py", "pkg1");
  }

  // PY-13870
  public void testConditionalImportFromPackage() {
    doMoveFileTest("pkg1/mod2.py", "");
  }

  // PY-14439
  public void testConditionalImportFromPackageToPackage() {
    doMoveFileTest("pkg1", "pkg2");
  }

  // PY-14979
  public void testTemplateAttributesExpansionInCreatedDestinationModule() {
    final FileTemplateManager instance = FileTemplateManager.getInstance(myFixture.getProject());
    final FileTemplate template = instance.getInternalTemplate("Python Script");
    assertNotNull(template);
    final String oldTemplateContent = template.getText();
    try {
      template.setText("NAME = '${NAME}'");
      doMoveSymbolTest("C", "b.py");
    }
    finally {
      template.setText(oldTemplateContent);
    }
  }

  // PY-7378
  public void testMoveNamespacePackage1() {
    runWithLanguageLevel(LanguageLevel.PYTHON33, new Runnable() {
      @Override
      public void run() {
        doMoveFileTest("nspkg/nssubpkg", "");
      }
    });
  }

  // PY-7378
  public void testMoveNamespacePackage2() {
    runWithLanguageLevel(LanguageLevel.PYTHON33, new Runnable() {
      @Override
      public void run() {
        doMoveFileTest("nspkg/nssubpkg/a.py", "");
      }
    });
  }

  // PY-7378
  public void testMoveNamespacePackage3() {
    runWithLanguageLevel(LanguageLevel.PYTHON33, new Runnable() {
      @Override
      public void run() {
        doMoveFileTest("nspkg/nssubpkg/a.py", "nspkg");
      }
    });
  }

  // PY-14384
  public void testRelativeImportInsideNamespacePackage() {
    runWithLanguageLevel(LanguageLevel.PYTHON33, new Runnable() {
      @Override
      public void run() {
        doMoveFileTest("nspkg/nssubpkg", "");
      }
    });
  }

  // PY-14384
  public void testRelativeImportInsideNormalPackage() {
    doMoveFileTest("nspkg/nssubpkg", "");
  }

  // PY-14432
  public void testRelativeImportsInsideMovedModule() {
    doMoveFileTest("pkg1/subpkg1", "");
  }


  // PY-14432
  public void testRelativeImportSourceWithSpacesInsideMovedModule() {
    doMoveFileTest("pkg/subpkg1/a.py", "");
  }

  // PY-14595
  public void testNamespacePackageUsedInMovedFunction() {
    runWithLanguageLevel(LanguageLevel.PYTHON33, new Runnable() {
      @Override
      public void run() {
        doMoveSymbolTest("func", "b.py");
      }
    });
  }

  // PY-14599
  public void testMoveFunctionFromUnimportableModule() {
    doMoveSymbolTest("func", "dst.py");
  }

  // PY-14599
  public void testMoveUnreferencedFunctionToUnimportableModule() {
    doMoveSymbolTest("func", "dst-unimportable.py");
  }

  // PY-14599
  public void testMoveReferencedFunctionToUnimportableModule() {
    try {
      doMoveSymbolTest("func", "dst-unimportable.py");
      fail();
    }
    catch (IncorrectOperationException e) {
      assertEquals("Cannot use module name 'dst-unimportable.py' in imports", e.getMessage());
    }
  }

  public void testRelativeImportOfNameFromInitPy() {
    doMoveFileTest("pkg/subpkg2", "");
  }

  // PY-15218
  public void testImportForMovedElementWithPreferredQualifiedImportStyle() {
    final boolean defaultImportStyle = PyCodeInsightSettings.getInstance().PREFER_FROM_IMPORT;
    try {
      PyCodeInsightSettings.getInstance().PREFER_FROM_IMPORT = false;
      doMoveSymbolTest("bar", "b.py");
    }
    finally {
      PyCodeInsightSettings.getInstance().PREFER_FROM_IMPORT = defaultImportStyle;
    }
  }

  private void doMoveFileTest(String fileName, String toDirName)  {
    Project project = myFixture.getProject();
    PsiManager manager = PsiManager.getInstance(project);

    String root = "/refactoring/move/" + getTestName(true);
    String rootBefore = root + "/before/src";
    String rootAfter = root + "/after/src";

    VirtualFile dir1 = myFixture.copyDirectoryToProject(rootBefore, "");
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    VirtualFile virtualFile = dir1.findFileByRelativePath(fileName);
    assertNotNull(virtualFile);
    PsiElement file = manager.findFile(virtualFile);
    if (file == null) {
      file = manager.findDirectory(virtualFile);
    }
    assertNotNull(file);
    VirtualFile toVirtualDir = dir1.findFileByRelativePath(toDirName);
    assertNotNull(toVirtualDir);
    PsiDirectory toDir = manager.findDirectory(toVirtualDir);
    new MoveFilesOrDirectoriesProcessor(project, new PsiElement[] {file}, toDir, false, false, null, null).run();

    VirtualFile dir2 = getVirtualFileByName(PythonTestUtil.getTestDataPath() + rootAfter);
    try {
      PlatformTestUtil.assertDirectoriesEqual(dir2, dir1);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void doMoveSymbolTest(String symbolName, String toFileName) {
    String root = "/refactoring/move/" + getTestName(true);
    String rootBefore = root + "/before/src";
    String rootAfter = root + "/after/src";
    VirtualFile dir1 = myFixture.copyDirectoryToProject(rootBefore, "");
    PsiDocumentManager.getInstance(myFixture.getProject()).commitAllDocuments();

    PsiNamedElement element = findFirstNamedElement(symbolName);
    assertNotNull(element);

    VirtualFile toVirtualFile = dir1.findFileByRelativePath(toFileName);
    String path = toVirtualFile != null ? toVirtualFile.getPath() : (dir1.getPath() + "/" + toFileName);
    new PyMoveModuleMembersProcessor(myFixture.getProject(),
                                       new PsiNamedElement[] {element},
                                       path,
                                       false).run();

    VirtualFile dir2 = getVirtualFileByName(PythonTestUtil.getTestDataPath() + rootAfter);
    try {
      PlatformTestUtil.assertDirectoriesEqual(dir2, dir1);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private PsiNamedElement findFirstNamedElement(String name) {
    final Project project = myFixture.getProject();
    final Collection<PyClass> classes = PyClassNameIndex.find(name, project, false);
    if (classes.size() > 0) {
      return classes.iterator().next();
    }
    final Collection<PyFunction> functions = PyFunctionNameIndex.find(name, project);
    if (functions.size() > 0) {
      return functions.iterator().next();
    }
    final Collection<PyTargetExpression> targets = PyVariableNameIndex.find(name, project, ProjectScope.getAllScope(project));
    if (targets.size() > 0) {
      return targets.iterator().next();
    }
    return null;
  }
}

