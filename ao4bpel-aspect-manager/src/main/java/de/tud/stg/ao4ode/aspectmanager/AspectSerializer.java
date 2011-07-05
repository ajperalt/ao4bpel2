package de.tud.stg.ao4ode.aspectmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.ode.bpel.o.OAspect;

public class AspectSerializer {

	
    public void writeOAspect(OAspect oaspect, OutputStream os) {

    	try {
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(oaspect);
			oos.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public OAspect readOAspect(InputStream is) {
		OAspect oaspect = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(is);			
			oaspect = (OAspect)ois.readObject();
			ois.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return oaspect;
	}

}
