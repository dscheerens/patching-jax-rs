package net.novazero.example.jaxrspatch.patchsupport;

public interface ObjectPatch {

	<T> T apply(T target) throws ObjectPatchException;
	
}
