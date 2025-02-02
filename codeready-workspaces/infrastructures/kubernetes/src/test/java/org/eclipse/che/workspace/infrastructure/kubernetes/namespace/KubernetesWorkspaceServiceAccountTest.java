/*
 * Copyright (c) 2012-2021 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.namespace;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingList;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.util.Collections;
import java.util.Set;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesClientFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(MockitoTestNGListener.class)
public class KubernetesWorkspaceServiceAccountTest {

  public static final String NAMESPACE = "testNamespace";
  public static final String WORKSPACE_ID = "workspace123";
  public static final String SA_NAME = "workspace-sa";
  public static final Set<String> ROLE_NAMES = Collections.singleton("role-foo");

  @Mock private KubernetesClientFactory clientFactory;
  private KubernetesClient k8sClient;
  private KubernetesServer serverMock;
  private KubernetesWorkspaceServiceAccount serviceAccount;

  @BeforeMethod
  public void setUp() throws Exception {
    this.serviceAccount =
        new KubernetesWorkspaceServiceAccount(
            WORKSPACE_ID, NAMESPACE, SA_NAME, ROLE_NAMES, clientFactory);

    serverMock = new KubernetesServer(true, true);
    serverMock.before();
    k8sClient = serverMock.getClient();
    when(clientFactory.create(anyString())).thenReturn(k8sClient);
  }

  @Test
  public void shouldProvisionSARolesEvenIfItAlreadyExists() throws Exception {
    ServiceAccountBuilder serviceAccountBuilder =
        new ServiceAccountBuilder().withNewMetadata().withName(SA_NAME).endMetadata();
    RoleBuilder roleBuilder = new RoleBuilder().withNewMetadata().withName("foo").endMetadata();
    RoleBindingBuilder roleBindingBuilder =
        new RoleBindingBuilder().withNewMetadata().withName("foo-builder").endMetadata();

    // pre-create SA and some roles
    k8sClient
        .serviceAccounts()
        .inNamespace(NAMESPACE)
        .createOrReplace(serviceAccountBuilder.build());
    k8sClient.rbac().roles().inNamespace(NAMESPACE).create(roleBuilder.build());
    k8sClient.rbac().roleBindings().inNamespace(NAMESPACE).create(roleBindingBuilder.build());

    // when
    serviceAccount.prepare();

    // then
    // make sure more roles added
    RoleList rl = k8sClient.rbac().roles().inNamespace(NAMESPACE).list();
    assertTrue(rl.getItems().size() > 1);

    RoleBindingList rbl = k8sClient.rbac().roleBindings().inNamespace(NAMESPACE).list();
    assertTrue(rbl.getItems().size() > 1);
  }
}
