/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.services.data;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kitodo.MockDatabase;
import org.kitodo.dto.AuthorizationDTO;
import org.kitodo.services.ServiceManager;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class AuthorizationServiceIT {

    private static final AuthorizationService authorizationService = new ServiceManager().getAuthorizationService();

    @BeforeClass
    public static void prepareDatabase() throws Exception {
        MockDatabase.startNode();
        MockDatabase.insertUserGroupsFull();
    }

    @AfterClass
    public static void cleanDatabase() throws Exception {
        MockDatabase.stopNode();
        MockDatabase.cleanDatabase();
    }

    @Before
    public void multipleInit() throws InterruptedException {
        Thread.sleep(500);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCountAllAuthorizations() throws Exception {
        Long amount = authorizationService.count();
        assertEquals("Authorizations were not counted correctly!", Long.valueOf(3), amount);
    }

    @Test
    public void shouldCountAllDatabaseRowsForAuthorizations() throws Exception {
        Long amount = authorizationService.countDatabaseRows();
        assertEquals("Authorizations were not counted correctly!", Long.valueOf(3), amount);
    }

    @Test
    public void shouldFindAllAuthorizations() throws Exception {
        List<AuthorizationDTO> authorizations = authorizationService.findAll();
        assertEquals("Not all authorizations were found in database!", 3, authorizations.size());
    }
}
