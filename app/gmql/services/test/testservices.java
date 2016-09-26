/*
 * Copyright (C) 2014 Abdulrahman Kaitoua <abdulrahman.kaitoua at polimi.it>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package gmql.services.test;


    
import java.net.URI;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import javax.net.ssl.SSLContext;

/**
 *
 * @author Abdulrahman Kaitoua <abdulrahman.kaitoua at polimi.it>
 */

public class testservices {
  public static void main(String[] args) throws NoSuchAlgorithmException {
    ClientConfig config = new DefaultClientConfig();

    Client client = Client.create(config);
//    client.addFilter(new HTTPBasicAuthFilter("abdulrahman", "mypass"));
    WebResource service = client.resource(getBaseURI());
//      WebResource webResource = client.resource("http://131.175.120.18:8080/gmql-services-1.0.4/");
//      String response = webResource.path("rest").path("datasets").path("listAll").accept(MediaType.TEXT_PLAIN).get(String.class);
//      System.out.println(response);
    // Get XML
    System.out.println(service/*.path("rest").path("datasets").path("listAll").*/
            .path("rest").path("datasets")/*.path("unRegisterUser").path("abdoTest").*/
            .path("listDSSamples").path("public.MM9_BED_ANNOTATION").path("public").
//            queryParam("query", "textbox").
//            queryParam("filename","test1").
//            queryParam("filekey","").
//            path("public").
            accept(MediaType.APPLICATION_XML/*MediaType.TEXT_XML*/).get(String.class));
    // Get XML for application
    //System.out.println(service.path("rest").path("todo").accept(MediaType.APPLICATION_JSON).get(String.class));
    // Get JSON for application
    //System.out.println(service.path("rest").path("todo").accept(MediaType.APPLICATION_XML).get(String.class));
  }

  private static URI getBaseURI() {
    return UriBuilder.fromUri("http://131.175.120.18:8081/gmql-services-1.0.4/").build();

//      return UriBuilder.fromUri("http://131.175.120.18/").build();
  }

} 