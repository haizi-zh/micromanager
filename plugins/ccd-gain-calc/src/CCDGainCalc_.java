import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import ij.IJ;
import ij.plugin.PlugIn;

public class CCDGainCalc_ implements PlugIn {

	@Override
	public void run(String arg0) {
		final URLClassLoader loader = (URLClassLoader) getClass()
				.getClassLoader();
		try {
			Class<?> cls = (Executors.newFixedThreadPool(1)
					.submit(new Callable<Class<?>>() {
						@Override
						public Class<?> call() throws Exception {
							Thread.currentThread()
									.setContextClassLoader(loader);
							return Class.forName("org.zephyre.CCDGainCalc");
						}
					})).get();
			Method method = cls.getMethod("run");
			method.invoke(cls.newInstance());
		} catch (InterruptedException | ExecutionException
				| InstantiationException | IllegalAccessException
				| NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException e) {
			IJ.log(e.toString());
		}
	}

}
