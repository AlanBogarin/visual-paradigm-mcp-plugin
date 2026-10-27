package com.brunnen.vp.usecase.mcp;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IUseCaseDiagramUIModel;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IAssociation;
import com.vp.plugin.model.IExtend;
import com.vp.plugin.model.IInclude;
import com.vp.plugin.model.IProject;
import com.vp.plugin.model.IUseCase;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests real API calls and rejects invalid requests before mutating a project. */
class UseCaseMcpToolsServiceTest {
  private UseCaseMcpToolsService service;
  private IProject project;
  private DiagramManager manager;
  private UseCaseMcpToolsService.ModelFactory factory;
  private IUseCaseDiagramUIModel diagram;

  @BeforeEach
  void setUp() {
    ApplicationManager app = mock(ApplicationManager.class);
    ProjectManager projects = mock(ProjectManager.class);
    project = mock(IProject.class);
    manager = mock(DiagramManager.class);
    factory = mock(UseCaseMcpToolsService.ModelFactory.class);
    diagram = mock(IUseCaseDiagramUIModel.class);
    when(app.getProjectManager()).thenReturn(projects);
    when(app.getDiagramManager()).thenReturn(manager);
    when(projects.getProject()).thenReturn(project);
    when(project.toDiagramArray()).thenReturn(new IDiagramUIModel[] {diagram});
    when(project.allLevelModelElementIterator())
        .thenAnswer(invocation -> java.util.Collections.emptyIterator());
    when(diagram.getName()).thenReturn("Test");
    when(diagram.toDiagramElementArray()).thenReturn(new IDiagramElement[0]);
    service = new UseCaseMcpToolsService(app, factory);
  }

  @Test
  void createsDiagramOnSwingThread() {
    when(manager.createDiagram(any()))
        .thenAnswer(
            invocation -> {
              assertTrue(SwingUtilities.isEventDispatchThread());
              return diagram;
            });
    assertTrue(service.createUseCaseDiagram("New").contains("New"));
    verify(diagram).setName("New");
    verify(manager).openDiagram(diagram);
  }

  @Test
  void createsRealActorAndUseCaseShapes() {
    IActor actor = mock(IActor.class);
    IUseCase useCase = mock(IUseCase.class);
    IDiagramElement actorShape = mock(IDiagramElement.class);
    IDiagramElement useCaseShape = mock(IDiagramElement.class);
    when(factory.createActor()).thenReturn(actor);
    when(factory.createUseCase()).thenReturn(useCase);
    when(manager.createDiagramElement(diagram, actor)).thenReturn(actorShape);
    when(manager.createDiagramElement(diagram, useCase)).thenReturn(useCaseShape);
    service.addActor("User", "Test");
    service.addUseCase("Login", "Test");
    verify(actor).setName("User");
    verify(useCase).setName("Login");
    verify(actorShape).setBounds(60, 60, 80, 100);
    verify(useCaseShape).setBounds(280, 60, 180, 70);
  }

  @Test
  void reusesExistingActorWithoutDuplicatingProjectName() {
    IActor existing = mock(IActor.class);
    when(existing.getName()).thenReturn("User");
    when(project.allLevelModelElementIterator())
        .thenAnswer(invocation -> java.util.List.of(existing).iterator());
    IDiagramElement actorShape = mock(IDiagramElement.class);
    when(manager.createDiagramElement(diagram, existing)).thenReturn(actorShape);
    service.addActor("User", "Test");
    verify(factory, never()).createActor();
    verify(manager).createDiagramElement(diagram, existing);
  }

  @Test
  void createsAssociationBetweenActualEndpoints() {
    IActor actor = mock(IActor.class);
    IUseCase useCase = mock(IUseCase.class);
    when(actor.getName()).thenReturn("User");
    when(useCase.getName()).thenReturn("Login");
    IDiagramElement from = shape(actor);
    IDiagramElement to = shape(useCase);
    when(diagram.toDiagramElementArray()).thenReturn(new IDiagramElement[] {from, to});
    IAssociation relationship = mock(IAssociation.class);
    when(factory.createAssociation()).thenReturn(relationship);
    service.addRelationship("User", "Login", "Association", "Test");
    verify(relationship).setFrom(actor);
    verify(relationship).setTo(useCase);
    verify(manager).createConnector(eq(diagram), eq(relationship), eq(from), eq(to), isNull());
  }

  @Test
  void createsIncludeBetweenUseCases() {
    IUseCase from = mock(IUseCase.class);
    IUseCase to = mock(IUseCase.class);
    when(from.getName()).thenReturn("Checkout");
    when(to.getName()).thenReturn("Pay");
    IDiagramElement fromShape = shape(from);
    IDiagramElement toShape = shape(to);
    when(diagram.toDiagramElementArray()).thenReturn(new IDiagramElement[] {fromShape, toShape});
    IInclude relationship = mock(IInclude.class);
    when(factory.createInclude()).thenReturn(relationship);
    service.addRelationship("Checkout", "Pay", "Include");
    verify(relationship).setFrom(from);
    verify(relationship).setTo(to);
  }

  @Test
  void extendsBaseWithReversedVisualParadigmEndpoints() {
    IUseCase extending = mock(IUseCase.class);
    IUseCase base = mock(IUseCase.class);
    when(extending.getName()).thenReturn("Apply coupon");
    when(base.getName()).thenReturn("Buy");
    IDiagramElement extendingShape = shape(extending);
    IDiagramElement baseShape = shape(base);
    when(diagram.toDiagramElementArray())
        .thenReturn(new IDiagramElement[] {extendingShape, baseShape});
    IExtend relationship = mock(IExtend.class);
    when(factory.createExtend()).thenReturn(relationship);
    service.addRelationship("Apply coupon", "Buy", "Extend", "Test");
    verify(relationship).setFrom(base);
    verify(relationship).setTo(extending);
    verify(manager)
        .createConnector(
            eq(diagram), eq(relationship), eq(baseShape), eq(extendingShape), isNull());
  }

  @Test
  void reportsExtendInUmlArrowDirection() {
    IUseCase extending = mock(IUseCase.class);
    IUseCase base = mock(IUseCase.class);
    when(extending.getName()).thenReturn("Apply coupon");
    when(base.getName()).thenReturn("Buy");
    IExtend relationship = mock(IExtend.class);
    when(relationship.getModelType()).thenReturn("Extend");
    when(relationship.getFrom()).thenReturn(base);
    when(relationship.getTo()).thenReturn(extending);
    IDiagramElement connector = shape(relationship);
    when(diagram.toDiagramElementArray()).thenReturn(new IDiagramElement[] {connector});
    assertTrue(service.generateReport("Test").contains("Apply coupon -> Buy"));
  }

  @Test
  void rejectsMissingOrAmbiguousDiagramsAndDuplicateNames() {
    assertThrows(IllegalArgumentException.class, () -> service.addActor("User", "Missing"));
    assertThrows(IllegalArgumentException.class, () -> service.createUseCaseDiagram("Test"));
    when(project.toDiagramArray()).thenReturn(new IDiagramUIModel[] {diagram, diagram});
    assertThrows(IllegalArgumentException.class, () -> service.addActor("User", "Test"));
    verify(factory, never()).createActor();
  }

  @Test
  void rejectsEmptyNamesAndUnsupportedRelationships() {
    assertThrows(IllegalArgumentException.class, () -> service.createUseCaseDiagram(null));
    assertThrows(IllegalArgumentException.class, () -> service.addUseCase(" ", "Test"));
    assertThrows(
        IllegalArgumentException.class, () -> service.addRelationship("A", "B", "Invalid"));
    assertThrows(
        IllegalArgumentException.class, () -> service.addRelationship("A", "A", "Include"));
  }

  @Test
  void reportReadsOnlyNamedDiagram() {
    IActor actor = mock(IActor.class);
    when(actor.getName()).thenReturn("User");
    when(actor.getModelType()).thenReturn("Actor");
    IDiagramElement actorShape = shape(actor);
    when(diagram.toDiagramElementArray()).thenReturn(new IDiagramElement[] {actorShape});
    String report = service.generateReport("Test");
    assertTrue(report.contains("Actor: User"));
    assertTrue(report.contains("Actors: 1"));
    assertTrue(report.contains("Use cases: 0"));
  }

  private IDiagramElement shape(com.vp.plugin.model.IModelElement model) {
    IDiagramElement shape = mock(IDiagramElement.class);
    when(shape.getModelElement()).thenReturn(model);
    return shape;
  }
}
