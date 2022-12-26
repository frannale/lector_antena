package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;

public class LogFile {
	
	static String log_path;
	static String log_path_pending;
	static int log_max_size;
	static FileWriter log_file;
	static FileWriter log_file_pending;

		// CONSTRUCTOR
		public LogFile(String path,int max_size){
			
			log_max_size = max_size * 58;
			log_path = "/home/user/LECTOR_RFID/log.txt";
			log_path_pending = "/home/user/LECTOR_RFID/pending_log.txt" ;		
		}
	
		// CARGA LECTURA DE UN TAG
		public void guardarLectura(String epc,String datetime){
			
			try {
				String fileContent;
				StringBuilder contentBuilder = new StringBuilder();
				try (BufferedReader br = new BufferedReader(new FileReader(log_path))) {

				    String sCurrentLine;
				    while ((sCurrentLine = br.readLine()) != null) 
				    {
				        contentBuilder.append(sCurrentLine).append("\n");
				    }
				} catch (IOException e) {
				    e.printStackTrace();
				}
				fileContent = contentBuilder.toString();
				
				String new_log = MessageFormat.format("FECHA: {0} EPC: {1}\n", datetime,epc);
				log_file = new FileWriter(log_path);
				log_file.write(new_log + fileContent);
				log_file.close();
				
			} catch (Exception e) {
				System.out.println("Error al guardar la lectura en el log");
				e.printStackTrace();
			}
			
			
		}
		
		// CARGA LECTURA DE UN TAG PENDIENTE
		public static void guardarLecturaPendiente(String epc,String datetime){
			
			try {
				String fileContent;
				StringBuilder contentBuilder = new StringBuilder();
				try (BufferedReader br = new BufferedReader(new FileReader(log_path_pending))) {

				    String sCurrentLine;
				    while ((sCurrentLine = br.readLine()) != null) 
				    {
				        contentBuilder.append(sCurrentLine).append('\n');
				    }
				} catch (IOException e) {
				    e.printStackTrace();
				}
				fileContent = contentBuilder.toString();
				
				String new_log = MessageFormat.format("{0}&{1}!\n", datetime,epc);	
				log_file_pending = new FileWriter(log_path_pending);
				log_file_pending.write(new_log + fileContent);
				log_file_pending.close();
			} catch (Exception e) {
				System.out.println("Error al guardar la lectura pendiente en el log");
				e.printStackTrace();
			}
		}

		// LECTURAS PENDIENTES
		public static String lecturasPendientes(){
			
			try {
				String fileContent;
				StringBuilder contentBuilder = new StringBuilder();
				try (BufferedReader br = new BufferedReader(new FileReader(log_path_pending))) {

				    String sCurrentLine;
				    while ((sCurrentLine = br.readLine()) != null) 
				    {
				        contentBuilder.append(sCurrentLine);
				    }
				} catch (IOException e) {
				    e.printStackTrace();
				}
				fileContent = contentBuilder.toString();
				
				return fileContent;
				
			} catch (Exception e) {
				System.out.println("Error al levantar pendientes");
				e.printStackTrace();
				return "";
			}
			
		}
		
		// REDUCE FILE A N BYTES
		public void verificarTamanio(){
			
			try {
				RandomAccessFile file = new RandomAccessFile (log_path,"rw");
				int file_length = (int) file.length();
				// SUPERA EL TAMANIO MAXIMO PERMITIDO
				if(file_length > log_max_size) {		
					file.setLength(log_max_size);
				}
			    file.close();
			    
				
			} catch (Exception e) {
				System.out.println("Error al verificar tamanio de log");
				e.printStackTrace();
			}
		    
		}
		
		// ELIMINA LECTURA DE UN TAG PENDIENTE
		public static void eliminarLecturaPendiente(String epc,String datetime){
			
			try {
				String fileContent;
				StringBuilder contentBuilder = new StringBuilder();
				try (BufferedReader br = new BufferedReader(new FileReader(log_path_pending))) {

				    String sCurrentLine;
				    while ((sCurrentLine = br.readLine()) != null) 
				    {
				        contentBuilder.append(sCurrentLine).append('\n');
				    }
				} catch (IOException e) {
				    e.printStackTrace();
				}
				fileContent = contentBuilder.toString();
				
				String new_log = MessageFormat.format("{0}&{1}!\n", datetime,epc);	
				log_file_pending = new FileWriter(log_path_pending);
				log_file_pending.write(fileContent.replaceFirst(new_log, ""));
				log_file_pending.close();
			} catch (Exception e) {
				System.out.println("Error al eliminar la lectura pendiente en el log");
				e.printStackTrace();
			}
		}

}
