package main;



import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

import com.google.gson.JsonObject;


public class ApiServer {
	
	static String  ip_destino;
	static String  id_cabina;
	static String  username;
	static String  password;
	static String  token;
	static JsonObject bodyLogin; 
	
	// CONSTRUCTOR
	public ApiServer(String host_destino,String cabina_id,String api_username,String api_password){
		
		ip_destino = host_destino;
		id_cabina = cabina_id;
		username = api_username;
		password = api_password;
		bodyLogin = new JsonObject();
		bodyLogin.addProperty("username", username);
		bodyLogin.addProperty("password", password);
		
		login();
	}

	// LOGIN
	public static void login(){

        Call<LoginResponse> login = ApiAdapter.getApiService().login(bodyLogin);
        login.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
            	LoginResponse respuesta = response.body();
                if (response.isSuccessful() && respuesta.getExito()) {
                	token = "Bearer " + respuesta.getValue();
                	System.out.println("La cabina inicio sesion en el servidor al arrancar!");
                }else {
                	System.out.println("Credenciales invalidas");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) { 
            	System.out.println("La cabina no pudo inicar sesion en el servidor al arrancar!")
            	;}
        });
	}
	
	// ENVIA LECTURA DE UN TAG PREVIO LOGIN
	public static void enviarLecturaEnLogin(final JsonObject lecturaBody, final String epc, final String fecha){
		
        Call<LoginResponse> login = ApiAdapter.getApiService().login(bodyLogin);
        login.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
            	LoginResponse respuesta = response.body();
                if (response.isSuccessful() && respuesta.getExito()) {
                	token = "Bearer " + respuesta.getValue();
                	// REENVIA LECTURA EN LOGIN EXITOSO
                	System.out.println("La cabina inicio sesion y enviara la lectura!");
                    Call<LecturaResponse> send = ApiAdapter.getApiService().sendLectura(token,lecturaBody);
                    
                    send.enqueue(new Callback<LecturaResponse>() {
                        @Override
                        public void onResponse(Call<LecturaResponse> call, Response<LecturaResponse> response) {
                        	LecturaResponse respuesta = response.body();
                            if (response.isSuccessful() && respuesta.getExito()) {
                            	System.out.println("Lectura enviada exitosamente!");
                                ApiServer.syncPendientes();
                            }else {
                            	System.out.println("Fallo al enviar la lectura!");
                            	LogFile.guardarLecturaPendiente(epc, fecha);
                            }
                        }
                        @Override
                        public void onFailure(Call<LecturaResponse> call, Throwable t) {
                        	token = null;
                        	System.out.println("Fallo al enviar la lectura!");
                        	LogFile.guardarLecturaPendiente(epc, fecha);
                        }
                    });
                }else{
                    System.out.println("Credenciales invalidas");
                    LogFile.guardarLecturaPendiente(epc, fecha);
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
            	System.out.println("La cabina no pudo iniciar sesion en el servidor!");
            	LogFile.guardarLecturaPendiente(epc, fecha);
            	token = null;
            }
        });
	}
	
	// ENVIA LECTURA DE UN TAG
	public static void enviarLectura(final String epc,final String datetime){
		
		final JsonObject body = new JsonObject();
        body.addProperty("id_cabina", id_cabina);
        body.addProperty("epc",  epc);
        body.addProperty("fecha_lectura", datetime);
        Call<LecturaResponse> send = ApiAdapter.getApiService().sendLectura(token,body);
        send.enqueue(new Callback<LecturaResponse>() {
            @Override
            public void onResponse(Call<LecturaResponse> call, Response<LecturaResponse> response) {
            	LecturaResponse respuesta = response.body();
                if (response.isSuccessful() && respuesta.getExito() ) {
                	System.out.println("Lectura enviada exitosamente!");
                    ApiServer.syncPendientes();
                }else{
                    System.out.println("Fallo el envio al servidor, se reeintentara nuevamente!");
                	ApiServer.enviarLecturaEnLogin(body,epc,datetime);
                }
            }

            @Override
            public void onFailure(Call<LecturaResponse> call, Throwable t) {
            	ApiServer.enviarLecturaEnLogin(body,epc,datetime);
            }
        });
	}


    	// ENVIA LECTURA DE LECTURAS PENDIENTES
	public static void syncPendientes(){
		String data = LogFile.lecturasPendientes();
		String[] element = data.split("!");
		String[] elementContent;
        for (String s: element) {
        	if(s.length() > 1) {
            	elementContent = s.split("&");
            	ApiServer.enviarLecturaPendiente(elementContent[1],elementContent[0]);
            	LogFile.eliminarLecturaPendiente(elementContent[1], elementContent[0]);
        	}
        	
        }
        
	}
	
    	
	// ENVIA LECTURA DE LECTURAS PENDIENTES
	public static void enviarLecturaPendiente(final String epc,final String datetime){
		
		final JsonObject body = new JsonObject();
        body.addProperty("id_cabina", id_cabina);
        body.addProperty("epc",  epc);
        body.addProperty("fecha_lectura", datetime);
        Call<LecturaResponse> send = ApiAdapter.getApiService().sendLectura(token,body);
        send.enqueue(new Callback<LecturaResponse>() {
            @Override
            public void onResponse(Call<LecturaResponse> call, Response<LecturaResponse> response) {
            	LecturaResponse respuesta = response.body();
                if (response.isSuccessful() && respuesta.getExito() ) {
                	System.out.println("Lectura pendiente enviada exitosamente!");
                }else{
                	System.out.println("Fallo el envio de lectura pendiente");
                }
            }

            @Override
            public void onFailure(Call<LecturaResponse> call, Throwable t) {
            	System.out.println("Fallo el envio de lectura pendiente");
            }
        });
	}

	
}
