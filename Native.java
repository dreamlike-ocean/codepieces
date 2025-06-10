package io.github.dreamlike.playground;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;

public class Native {
    public static void main(String[] args) throws Throwable {
        Class<?> linkerClass = Class.forName("java.lang.foreign.Linker");
        Class<?> symbolLookupClass = Class.forName("java.lang.foreign.SymbolLookup");
        Class<?> optionalClass = Class.forName("java.util.Optional");
        Class<?> memorySegmentClass = Class.forName("java.lang.foreign.MemorySegment");

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle optionalGetMH = lookup.findVirtual(optionalClass, "get", MethodType.methodType(Object.class));

        //  Linker.nativeLinker()
        // () -> Linker
        MethodHandle nativeLinkerMH = lookup.findStatic(linkerClass, "nativeLinker", MethodType.methodType(linkerClass));
        // SymbolLookup.defaultLookup()
        // Linker -> SymbolLookup
        MethodHandle defaultLookupMH = lookup.findVirtual(linkerClass, "defaultLookup", MethodType.methodType(symbolLookupClass));
        MethodHandle getSymbolLookupMH = MethodHandles.filterReturnValue(
                nativeLinkerMH,
                defaultLookupMH
        );

        // (SymbolLookup, String) -> Optional<MemorySegment>
        MethodHandle findMH = lookup.findVirtual(symbolLookupClass, "find", MethodType.methodType(optionalClass, String.class));

        // (SymbolLookup, String) -> T(MemorySegment)
        MethodHandle getFpMh = MethodHandles.filterReturnValue(
                findMH,
                optionalGetMH.asType(optionalGetMH.type().changeReturnType(memorySegmentClass))
        );

        // (Linker.nativeLinker().defaultLookup(), String) -> MemorySegment
        MethodHandle findFpMH = MethodHandles.foldArguments(getFpMh, 0, getSymbolLookupMH);

        // () -> MemorySegment(malloc fp)
        MethodHandle findMallocFpMH = findFpMH.bindTo("malloc");

        Class<?> functionDescriptorClass = Class.forName("java.lang.foreign.FunctionDescriptor");
        Class<?> linkerOptionsClass = Class.forName("java.lang.foreign.Linker$Option");
        Class<?> linkerOptionsArrayClass = linkerOptionsClass.arrayType();
        Class<?> valueLayoutClass = Class.forName("java.lang.foreign.ValueLayout");
        Class<?> addressLayoutClass = Class.forName("java.lang.foreign.AddressLayout");
        Class<?> longLayoutClass = Class.forName("java.lang.foreign.ValueLayout$OfLong");
        Class<?> memoryLayoutClass = Class.forName("java.lang.foreign.MemoryLayout");

        MethodHandle getAddressValueLayoutMH = lookup.findStaticGetter(valueLayoutClass, "ADDRESS", addressLayoutClass)
                .asType(MethodType.methodType(memoryLayoutClass));
        MethodHandle getLongValueLayoutMH = lookup.findStaticGetter(valueLayoutClass, "JAVA_LONG", longLayoutClass)
                .asType(MethodType.methodType(memoryLayoutClass));
        // (memoryLayout, memoryLayout[]) -> FunctionDescriptor
        Class<?> memoryLayoutAddressClass = memoryLayoutClass.arrayType();
        MethodHandle getFunctionDescriptorMH = lookup.findStatic(functionDescriptorClass, "of", MethodType.methodType(functionDescriptorClass, memoryLayoutClass, memoryLayoutAddressClass));
        // (ValueLayout.ADDRESS, memoryLayout[]) -> FunctionDescriptor
        MethodHandle mallocFunctionDescriptorMH = MethodHandles.foldArguments(
                getFunctionDescriptorMH,
                0,
                getAddressValueLayoutMH
        );

        Object longLayout = getLongValueLayoutMH.invoke();
        Object paramArray = Array.newInstance(memoryLayoutClass, 1);  // new MemoryLayout[1]
        Array.set(paramArray, 0, longLayout);                         // arr[0] = JAVA_LONG
        MethodHandle constParamsMH = MethodHandles.constant(
                memoryLayoutAddressClass,
                paramArray
        );
        mallocFunctionDescriptorMH = MethodHandles.foldArguments(
                mallocFunctionDescriptorMH,
                0,
                constParamsMH
        );

        Object emptyLinkerOptions = Array.newInstance(linkerOptionsClass, 0);

        MethodHandle downcallHandleMH = lookup.findVirtual(linkerClass, "downcallHandle", MethodType.methodType(MethodHandle.class, memorySegmentClass, functionDescriptorClass, linkerOptionsArrayClass));
        MethodHandle mallocHandleDownCallMH = MethodHandles.foldArguments(
                downcallHandleMH,
                0,
                nativeLinkerMH
        );

        mallocHandleDownCallMH = MethodHandles.foldArguments(
                mallocHandleDownCallMH,
                0,
                findMallocFpMH
        );
        mallocHandleDownCallMH = MethodHandles.foldArguments(
                mallocHandleDownCallMH,
                0,
                mallocFunctionDescriptorMH
        );
        mallocHandleDownCallMH = MethodHandles.foldArguments(
                mallocHandleDownCallMH,
                0,
                MethodHandles.constant(
                        linkerOptionsArrayClass,
                        emptyLinkerOptions
                )
        );

        MethodHandle mallocMethodHandle = (MethodHandle) mallocHandleDownCallMH.invokeExact();
        System.out.println(mallocMethodHandle.invoke(100));
    }

}
