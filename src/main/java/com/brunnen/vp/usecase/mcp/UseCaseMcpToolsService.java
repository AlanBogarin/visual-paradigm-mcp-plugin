package com.brunnen.vp.usecase.mcp;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.diagram.IConnectorUIModel;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramTypeConstants;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IShapeUIModel;
import com.vp.plugin.diagram.IUseCaseDiagramUIModel;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IAssociation;
import com.vp.plugin.model.IExtend;
import com.vp.plugin.model.IInclude;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import com.vp.plugin.model.IRelationship;
import com.vp.plugin.model.IUseCase;
import com.vp.plugin.model.factory.IModelElementFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;

/** Real Visual Paradigm operations, serialized on the Swing event dispatch thread. */
public class UseCaseMcpToolsService {
  private final ApplicationManager application;
  private final ModelFactory modelFactory;

  /** Creates a service that resolves the Visual Paradigm factory only when a tool is called. */
  public UseCaseMcpToolsService() {
    this(
        null,
        new ModelFactory() {
          public IActor createActor() {
            return IModelElementFactory.instance().createActor();
          }

          public IUseCase createUseCase() {
            return IModelElementFactory.instance().createUseCase();
          }

          public IAssociation createAssociation() {
            return IModelElementFactory.instance().createAssociation();
          }

          public IInclude createInclude() {
            return IModelElementFactory.instance().createInclude();
          }

          public IExtend createExtend() {
            return IModelElementFactory.instance().createExtend();
          }
        });
  }

  UseCaseMcpToolsService(ApplicationManager application, ModelFactory modelFactory) {
    this.application = application;
    this.modelFactory = modelFactory;
  }

  private ApplicationManager application() {
    ApplicationManager result = application == null ? ApplicationManager.instance() : application;
    if (result == null) {
      throw new IllegalStateException("Visual Paradigm is not ready. Open a project first.");
    }
    return result;
  }

  private ModelFactory factory() {
    return modelFactory;
  }

  interface ModelFactory {
    IActor createActor();

    IUseCase createUseCase();

    IAssociation createAssociation();

    IInclude createInclude();

    IExtend createExtend();
  }

  private IProject project() {
    IProject result = application().getProjectManager().getProject();
    if (result == null) {
      throw new IllegalStateException(
          "No project is open. Open a project in Visual Paradigm first.");
    }
    return result;
  }

  /** Creates and opens a uniquely named diagram in the current project. */
  public String createUseCaseDiagram(String diagramName) {
    String name = name(diagramName);
    return onEdt(
        () -> {
          IProject current = project();
          for (IDiagramUIModel diagram : current.toDiagramArray()) {
            if (name.equals(diagram.getName())) {
              throw new IllegalArgumentException("A diagram already has this name: " + name);
            }
          }
          DiagramManager manager = application().getDiagramManager();
          IDiagramUIModel diagram =
              manager.createDiagram(IDiagramTypeConstants.DIAGRAM_TYPE_USE_CASE_DIAGRAM);
          diagram.setName(name);
          manager.openDiagram(diagram);
          return "Created use case diagram: " + name + " (id=" + diagram.getId() + ")";
        });
  }

  /** Creates an actor model and places its shape in the named diagram. */
  public String addActor(String actorName, String diagramName) {
    String name = name(actorName);
    String target = name(diagramName);
    return onEdt(
        () -> {
          IUseCaseDiagramUIModel diagram = diagram(target);
          ensureUniqueElement(diagram, name);
          int index = count(diagram, IActor.class);
          IActor actor = existingModel(name, IActor.class);
          if (actor == null) {
            actor = factory().createActor();
            actor.setName(name);
          }
          IDiagramElement shape =
              application().getDiagramManager().createDiagramElement(diagram, actor);
          shape.setBounds(60, 60 + index * 130, 80, 100);
          if (shape instanceof IShapeUIModel) {
            ((IShapeUIModel) shape).getCaptionUIModel().setBounds(40, 165 + index * 130, 120, 24);
          }
          return "Added actor '"
              + name
              + "' to diagram '"
              + target
              + "' (id="
              + actor.getId()
              + ")";
        });
  }

  /** Creates a use case model and places its shape in the named diagram. */
  public String addUseCase(String useCaseName, String diagramName) {
    String name = name(useCaseName);
    String target = name(diagramName);
    return onEdt(
        () -> {
          IUseCaseDiagramUIModel diagram = diagram(target);
          ensureUniqueElement(diagram, name);
          int index = count(diagram, IUseCase.class);
          IUseCase useCase = existingModel(name, IUseCase.class);
          if (useCase == null) {
            useCase = factory().createUseCase();
            useCase.setName(name);
          }
          IDiagramElement shape =
              application().getDiagramManager().createDiagramElement(diagram, useCase);
          shape.setBounds(280 + (index % 2) * 300, 60 + (index / 2) * 200, 180, 70);
          return "Added use case '"
              + name
              + "' to diagram '"
              + target
              + "' (id="
              + useCase.getId()
              + ")";
        });
  }

  /** Finds a unique diagram containing both endpoints and creates a relationship. */
  public String addRelationship(String actorName, String useCaseName, String relationshipType) {
    return addRelationship(actorName, useCaseName, relationshipType, null);
  }

  /** actorName is also the source use case name for Include and Extend. */
  public String addRelationship(
      String actorName, String useCaseName, String relationshipType, String diagramName) {
    String sourceName = name(actorName);
    String targetName = name(useCaseName);
    String type = name(relationshipType).toLowerCase(Locale.ROOT);
    if (!List.of("association", "include", "extend").contains(type)) {
      throw new IllegalArgumentException(
          "relationshipType must be Association, Include or Extend.");
    }
    if (sourceName.equals(targetName)) {
      throw new IllegalArgumentException("Source and target must be different.");
    }
    return onEdt(
        () -> {
          IUseCaseDiagramUIModel diagram = relationshipDiagram(diagramName, sourceName, targetName);
          IDiagramElement source = element(diagram, sourceName);
          IDiagramElement target = element(diagram, targetName);
          IModelElement from = source.getModelElement();
          IModelElement to = target.getModelElement();
          if (!(to instanceof IUseCase)
              || ("association".equals(type)
                  ? !(from instanceof IActor)
                  : !(from instanceof IUseCase))) {
            throw new IllegalArgumentException(
                "Association needs Actor -> UseCase; Include/Extend need UseCase -> UseCase.");
          }
          // VP stores Extend as base -> extending case, opposite to the UML arrow.
          boolean extend = "extend".equals(type);
          IModelElement vpFrom = extend ? to : from;
          IModelElement vpTo = extend ? from : to;
          for (IDiagramElement shape : diagram.toDiagramElementArray()) {
            IModelElement model = shape.getModelElement();
            if (model instanceof IRelationship) {
              IRelationship existing = (IRelationship) model;
              if (type.equalsIgnoreCase(model.getModelType())
                  && vpFrom.equals(existing.getFrom())
                  && vpTo.equals(existing.getTo())) {
                throw new IllegalArgumentException("This relationship already exists.");
              }
            }
          }
          IRelationship relationship;
          if ("association".equals(type)) {
            relationship = factory().createAssociation();
          } else {
            relationship =
                "include".equals(type) ? factory().createInclude() : factory().createExtend();
          }
          relationship.setFrom(vpFrom);
          relationship.setTo(vpTo);
          IDiagramElement connector =
              application()
                  .getDiagramManager()
                  .createConnector(
                      diagram,
                      relationship,
                      extend ? target : source,
                      extend ? source : target,
                      null);
          if (connector instanceof IConnectorUIModel && !"association".equals(type)) {
            int x = (source.getX() + target.getX()) / 2;
            int y = (source.getY() + target.getY()) / 2;
            if (source.getX() == target.getX()) {
              x -= 110;
              y += 35;
            } else {
              x += 90;
              y -= 5;
            }
            ((IConnectorUIModel) connector).getCaptionUIModel().setBounds(x, y, 100, 24);
          }
          return "Added "
              + type
              + " relationship between '"
              + sourceName
              + "' and '"
              + targetName
              + "' (id="
              + relationship.getId()
              + ")";
        });
  }

  /** Returns the actual models and relationships visible in the named diagram. */
  public String generateReport(String diagramName) {
    String target = name(diagramName);
    return onEdt(
        () -> {
          IUseCaseDiagramUIModel diagram = diagram(target);
          StringBuilder report = new StringBuilder("Diagram: ").append(target).append("\n");
          for (IDiagramElement shape : diagram.toDiagramElementArray()) {
            IModelElement model = shape.getModelElement();
            if (model == null) {
              continue;
            }
            report
                .append(model.getModelType())
                .append(": ")
                .append(model.getName() == null ? "" : model.getName());
            if (model instanceof IRelationship) {
              IRelationship relationship = (IRelationship) model;
              IModelElement from =
                  model instanceof IExtend ? relationship.getTo() : relationship.getFrom();
              IModelElement to =
                  model instanceof IExtend ? relationship.getFrom() : relationship.getTo();
              report
                  .append(" [")
                  .append(from.getName())
                  .append(" -> ")
                  .append(to.getName())
                  .append("]");
            }
            report.append(" (id=").append(model.getId()).append(")\n");
          }
          report
              .append("Actors: ")
              .append(count(diagram, IActor.class))
              .append("\nUse cases: ")
              .append(count(diagram, IUseCase.class));
          return report.toString();
        });
  }

  private IUseCaseDiagramUIModel diagram(String name) {
    List<IUseCaseDiagramUIModel> matches = new ArrayList<>();
    for (IDiagramUIModel diagram : project().toDiagramArray()) {
      if (diagram instanceof IUseCaseDiagramUIModel && name.equals(diagram.getName())) {
        matches.add((IUseCaseDiagramUIModel) diagram);
      }
    }
    if (matches.size() != 1) {
      throw new IllegalArgumentException(
          "Expected one use case diagram named '" + name + "'; found " + matches.size());
    }
    return matches.get(0);
  }

  private IUseCaseDiagramUIModel relationshipDiagram(String name, String from, String to) {
    if (name != null) {
      return diagram(name(name));
    }
    List<IUseCaseDiagramUIModel> matches = new ArrayList<>();
    for (IDiagramUIModel candidate : project().toDiagramArray()) {
      if (candidate instanceof IUseCaseDiagramUIModel
          && hasElement(candidate, from)
          && hasElement(candidate, to)) {
        matches.add((IUseCaseDiagramUIModel) candidate);
      }
    }
    if (matches.size() != 1) {
      throw new IllegalArgumentException(
          "Specify diagramName: expected exactly one diagram containing both endpoints.");
    }
    return matches.get(0);
  }

  private boolean hasElement(IDiagramUIModel diagram, String name) {
    for (IDiagramElement shape : diagram.toDiagramElementArray()) {
      if (shape.getModelElement() != null && name.equals(shape.getModelElement().getName())) {
        return true;
      }
    }
    return false;
  }

  private IDiagramElement element(IDiagramUIModel diagram, String name) {
    List<IDiagramElement> matches = new ArrayList<>();
    for (IDiagramElement shape : diagram.toDiagramElementArray()) {
      if (shape.getModelElement() != null && name.equals(shape.getModelElement().getName())) {
        matches.add(shape);
      }
    }
    if (matches.size() != 1) {
      throw new IllegalArgumentException(
          "Expected one element named '" + name + "' in this diagram; found " + matches.size());
    }
    return matches.get(0);
  }

  private <T extends IModelElement> T existingModel(String name, Class<T> type) {
    T match = null;
    Iterator<?> models = project().allLevelModelElementIterator();
    while (models.hasNext()) {
      Object value = models.next();
      if (value instanceof IModelElement && name.equals(((IModelElement) value).getName())) {
        if (!type.isInstance(value) || match != null) {
          throw new IllegalArgumentException(
              "The name is ambiguous or belongs to another model type: " + name);
        }
        match = type.cast(value);
      }
    }
    return match;
  }

  private void ensureUniqueElement(IDiagramUIModel diagram, String name) {
    if (hasElement(diagram, name)) {
      throw new IllegalArgumentException(
          "An element already has this name in the diagram: " + name);
    }
  }

  private int count(IDiagramUIModel diagram, Class<?> type) {
    int result = 0;
    for (IDiagramElement shape : diagram.toDiagramElementArray()) {
      if (type.isInstance(shape.getModelElement())) {
        result++;
      }
    }
    return result;
  }

  private static String name(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Names and relationship types must be nonempty strings.");
    }
    return value.trim();
  }

  private static <T> T onEdt(Callable<T> operation) {
    FutureTask<T> task = new FutureTask<>(operation);
    if (SwingUtilities.isEventDispatchThread()) {
      task.run();
    } else {
      SwingUtilities.invokeLater(task);
    }
    try {
      return task.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for Visual Paradigm.", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new IllegalStateException("Visual Paradigm operation failed.", cause);
    }
  }
}
