/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.kernel.services;

import static java.util.Objects.requireNonNull;
import javax.inject.Inject;
import org.opentcs.access.Kernel;
import org.opentcs.access.LocalKernel;
import org.opentcs.components.kernel.Dispatcher;
import org.opentcs.components.kernel.Router;
import org.opentcs.components.kernel.services.RouterService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Path;
import org.opentcs.kernel.KernelApplicationConfiguration;
import org.opentcs.kernel.workingset.PlantModelManager;

/**
 * This class is the standard implementation of the {@link RouterService} interface.
 */
public class StandardRouterService
    implements RouterService {

  /**
   * A global object to be used for synchronization within the kernel.
   */
  private final Object globalSyncObject;
  /**
   * The kernel.
   */
  private final LocalKernel kernel;
  /**
   * The router.
   */
  private final Router router;
  /**
   * The dispatcher.
   */
  private final Dispatcher dispatcher;
  /**
   * The plant model manager.
   */
  private final PlantModelManager plantModelManager;
  /**
   * The kernel application's configuration.
   */
  private final KernelApplicationConfiguration configuration;

  /**
   * Creates a new instance.
   *
   * @param globalSyncObject The kernel threads' global synchronization object.
   * @param kernel The kernel.
   * @param router The scheduler.
   * @param dispatcher The dispatcher.
   * @param plantModelManager The plant model manager to be used.
   * @param configuration The kernel application's configuration.
   */
  @Inject
  public StandardRouterService(@GlobalSyncObject Object globalSyncObject,
                               LocalKernel kernel,
                               Router router,
                               Dispatcher dispatcher,
                               PlantModelManager plantModelManager,
                               KernelApplicationConfiguration configuration) {
    this.globalSyncObject = requireNonNull(globalSyncObject, "globalSyncObject");
    this.kernel = requireNonNull(kernel, "kernel");
    this.router = requireNonNull(router, "router");
    this.dispatcher = requireNonNull(dispatcher, "dispatcher");
    this.plantModelManager = requireNonNull(plantModelManager, "plantModelManager");
    this.configuration = requireNonNull(configuration, "configuration");
  }

  @Override
  public void updatePathLock(TCSObjectReference<Path> ref, boolean locked)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      plantModelManager.setPathLocked(ref, locked);
      if (kernel.getState() == Kernel.State.OPERATING
          && configuration.updateRoutingTopologyOnPathLockChange()) {
        updateRoutingTopology();
      }
    }
  }

  @Override
  public void updateRoutingTopology() {
    synchronized (globalSyncObject) {
      router.topologyChanged();
      dispatcher.topologyChanged();
    }
  }
}
